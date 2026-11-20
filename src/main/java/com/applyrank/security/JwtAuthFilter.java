package com.applyrank.security;

import io.jsonwebtoken.Claims;
import com.applyrank.repo.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Runs on every request: if a valid Bearer token is present, the request becomes "logged in". */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                String role = claims.get("role", String.class);
                // A token is only good while its version number is the same as the user's current number.
                // (Tokens made before this feature have no "v", so I treat them as version 0.)
                Integer v = claims.get("v", Integer.class);
                int tokenVersion = v == null ? 0 : v;
                var user = users.findByEmail(claims.getSubject()).orElse(null);
                if (user == null || user.getTokenVersion() != tokenVersion) {
                    chain.doFilter(request, response);       // logged out / password reset / deleted account: stay anonymous (401)
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                // bad / expired token: the request stays anonymous and protected URLs answer 401
            }
        }
        chain.doFilter(request, response);
    }
}
