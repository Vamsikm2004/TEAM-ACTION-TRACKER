package com.teamactiontracker.backend.service;

import com.teamactiontracker.backend.dto.LoginRequest;
import com.teamactiontracker.backend.dto.LoginResponse;
import com.teamactiontracker.backend.dto.UserResponse;
import com.teamactiontracker.backend.model.User;
import com.teamactiontracker.backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> ALLOWED_ROLES =
            Set.of("MANAGER", "TEAM_MEMBER");

    private static final String INVALID_CREDENTIALS =
            "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isEmpty()
                || request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new BadCredentialsException(INVALID_CREDENTIALS));

        // Verify account status and password.
        if (!Boolean.TRUE.equals(user.getActiveStatus())
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash())) {

            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        // Validate the user's assigned role.
        String role = user.getRole() == null
                ? ""
                : user.getRole().trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_ROLES.contains(role)) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        // Create authenticated principal.
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getEmail(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        )
                );

        // Create and populate the security context.
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Create a session and rotate its ID.
        httpRequest.getSession(true);
        httpRequest.changeSessionId();

        // Persist the security context for subsequent requests.
        contextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return new LoginResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                role,
                "Login successful"
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(found -> Boolean.TRUE.equals(found.getActiveStatus()))
                .orElseThrow(() ->
                        new BadCredentialsException(INVALID_CREDENTIALS));

        String role = user.getRole() == null
                ? ""
                : user.getRole().trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_ROLES.contains(role)) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                role,
                "Session active"
        );
    }

}
