package com.applyrank.web;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.security.JwtService;
import com.applyrank.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import com.applyrank.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final String dummyHash;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Mapper mapper;
    private final CurrentUser current;
    private final NotificationService notes;
    private final LoginAttemptService attempts;
    private final AuditService audit;
    private final TokenService tokens;
    private final EmailService email;

    public AuthController(UserRepository users, ProfileRepository profiles, PasswordEncoder encoder, JwtService jwt,
                          Mapper mapper, CurrentUser current, NotificationService notes,
                          LoginAttemptService attempts, AuditService audit,
                          TokenService tokens, EmailService email) {
        this.tokens = tokens; this.email = email;
        this.attempts = attempts; this.audit = audit; this.dummyHash = encoder.encode("not-a-real-password");
        this.users = users; this.profiles = profiles; this.encoder = encoder; this.jwt = jwt;
        this.mapper = mapper; this.current = current; this.notes = notes;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r, HttpServletRequest req) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "This email is already registered. Please log in.");
        User u = new User();
        u.setEmail(email);
        u.setName(r.name().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole("RECRUITER".equalsIgnoreCase(r.role()) ? Role.RECRUITER : Role.CANDIDATE);   // open for demo - see README
        users.save(u);
        Profile p = new Profile();
        p.setUser(u);
        profiles.save(p);
        audit.record(u.getEmail(), "REGISTER", "role=" + u.getRole(), req.getRemoteAddr());
        notes.push(u, "updates", "Welcome to ApplyRank!", "Take the skill test to earn your verified rank.",
                "Take the test", "link", "skilltest.html");
        sendVerification(u);
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name(), u.getTokenVersion()), mapper.user(u));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        String ip = req.getRemoteAddr(), email = r.email().trim().toLowerCase();
        if (attempts.isLocked(email, ip)) {
            audit.record(email, "LOGIN_LOCKED", "too many attempts", ip);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong attempts. Try again in " + (attempts.secondsLeft(email, ip) / 60 + 1) + " minutes.");
        }
        User u = users.findByEmail(email).orElse(null);
        // Always run BCrypt (even for unknown e-mails) so response time does not reveal which e-mails exist.
        boolean ok = encoder.matches(r.password(), u == null ? dummyHash : u.getPasswordHash()) && u != null;
        if (!ok) {
            attempts.failed(email, ip);
            audit.record(email, "LOGIN_FAIL", null, ip);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password.");
        }
        attempts.succeeded(email, ip);
        audit.record(email, "LOGIN_OK", null, ip);
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name(), u.getTokenVersion()), mapper.user(u));
    }

    private void sendVerification(User u) {
        String t = tokens.issue(u, "VERIFY", Duration.ofHours(24));
        email.sendLink(u.getEmail(), "Verify your ApplyRank email", "Welcome to ApplyRank! Please confirm your e-mail address:", "verify-email.html", t);
    }

    /** Always answers the same text, so nobody can use this to find out which e-mails are registered. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgot(@Valid @RequestBody ForgotRequest r, HttpServletRequest req) {
        String e = r.email().trim().toLowerCase();
        users.findByEmail(e).ifPresent(u -> {
            if (!tokens.recentlyIssued(u, "RESET", Duration.ofMinutes(1))) {          // max 1 mail per minute per account
                String t = tokens.issue(u, "RESET", Duration.ofMinutes(30));
                email.sendLink(u.getEmail(), "Reset your ApplyRank password", "Someone (hopefully you) asked to reset your password. This link works for 30 minutes:", "reset-password.html", t);
                audit.record(e, "PASSWORD_RESET_REQUESTED", null, req.getRemoteAddr());
            }
        });
        return Map.of("message", "If this email is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> reset(@Valid @RequestBody ResetRequest r, HttpServletRequest req) {
        AuthToken t = tokens.consume(r.token(), "RESET")
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired. Please ask for a new one."));
        User u = t.getUser();
        u.setPasswordHash(encoder.encode(r.password()));
        u.bumpTokenVersion();                // everyone who was logged in with the old password is logged out now
        users.save(u);
        attempts.succeeded(u.getEmail(), req.getRemoteAddr());
        audit.record(u.getEmail(), "PASSWORD_RESET_DONE", null, req.getRemoteAddr());
        notes.push(u, "updates", "Password changed", "Your password was just changed. If this was not you, reset it again right away.",
                "Open", "link", "index.html");
        return Map.of("message", "Password changed. You can log in now.");
    }

    /**
     * "Log out everywhere": adding 1 to the version makes every token of this user (this browser and any other
     * device) invalid immediately. Before this, logout only deleted the token in the browser.
     */
    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest req) {
        User u = current.get();
        u.bumpTokenVersion();
        users.save(u);
        audit.record(u.getEmail(), "LOGOUT", "all sessions ended", req.getRemoteAddr());
        return Map.of("message", "Logged out.");
    }

    @PostMapping("/verify-email")
    public Map<String, String> verify(@Valid @RequestBody VerifyRequest r, HttpServletRequest req) {
        AuthToken t = tokens.consume(r.token(), "VERIFY")
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This verification link is invalid or has expired."));
        User u = t.getUser();
        u.setEmailVerified(true);
        users.save(u);
        audit.record(u.getEmail(), "EMAIL_VERIFIED", null, req.getRemoteAddr());
        return Map.of("message", "Email verified. Thank you!");
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resend() {
        User u = current.get();
        if (u.isEmailVerified()) return Map.of("message", "Your email is already verified.");
        if (!tokens.recentlyIssued(u, "VERIFY", Duration.ofMinutes(1))) sendVerification(u);
        return Map.of("message", "Verification email sent.");
    }

    @GetMapping("/me")
    public UserDto me() {
        return mapper.user(current.get());
    }
}
