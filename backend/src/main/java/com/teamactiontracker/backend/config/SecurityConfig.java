package com.teamactiontracker.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource)
            throws Exception {

        http

            // CORS configuration.
            .cors(cors ->
                    cors.configurationSource(corsConfigurationSource)
            )

            // Retain CSRF protection for session-based authentication.
            .csrf(Customizer.withDefaults())

            // Create sessions only when required.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.IF_REQUIRED
                    )
            )

            // Disable default authentication mechanisms.
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())

            // Return HTTP 401 instead of redirecting API clients.
            .exceptionHandling(exception ->
                    exception.authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                    )
            )

            // Actual logout implementation.
            .logout(logout ->
                    logout
                            .logoutUrl("/api/auth/logout")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                            .deleteCookies("JSESSIONID")
                            .logoutSuccessHandler(
                                    (request, response, authentication) ->
                                            response.setStatus(
                                                    HttpStatus.NO_CONTENT.value()
                                            )
                            )
            )

            // Endpoint authorization.
            .authorizeHttpRequests(auth -> auth

                    // Public authentication endpoints.
                    .requestMatchers(
                            "/api/auth/login",
                            "/api/auth/csrf",
                            "/api/auth/logout"
                    ).permitAll()

                    // Public health endpoints.
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/health",
                            "/actuator/health"
                    ).permitAll()

                    // Public Swagger documentation.
                    .requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**"
                    ).permitAll()

                    // Browser CORS preflight.
                    .requestMatchers(
                            HttpMethod.OPTIONS,
                            "/**"
                    ).permitAll()

                    // User management: Managers only.
                    .requestMatchers(
                            "/api/users/**"
                    ).hasRole("MANAGER")

                    // Action item retrieval.
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/action-items",
                            "/api/action-items/**"
                    ).hasAnyRole("MANAGER", "TEAM_MEMBER")

                    // Action item creation: Managers only.
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/action-items",
                            "/api/action-items/**"
                    ).hasRole("MANAGER")

                    // Action item editing: Managers only.
                    .requestMatchers(
                            HttpMethod.PUT,
                            "/api/action-items",
                            "/api/action-items/**"
                    ).hasRole("MANAGER")

                    // Status updates: Managers and Team Members.
                    .requestMatchers(
                            HttpMethod.PATCH,
                            "/api/action-items/*/status"
                    ).hasAnyRole("MANAGER", "TEAM_MEMBER")

                    // All remaining requests require authentication.
                    .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
