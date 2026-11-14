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

    public AuthController(UserRepository users, ProfileRepository profiles, PasswordEncoder encoder, JwtService jwt,
                          Mapper mapper, CurrentUser current, NotificationService notes,
                          LoginAttemptService attempts, AuditService audit) {
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
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()), mapper.user(u));
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
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()), mapper.user(u));
    }

    @GetMapping("/me")
    public UserDto me() {
        return mapper.user(current.get());
    }
}
