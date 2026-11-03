package com.teamactiontracker.backend.controller;

import com.teamactiontracker.backend.dto.LoginRequest;
import com.teamactiontracker.backend.dto.LoginResponse;
import com.teamactiontracker.backend.dto.UserResponse;
import com.teamactiontracker.backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "APIs for user authentication and session management"
)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user",
            description = "Returns the profile associated with the current authenticated session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public UserResponse getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in a user",
            description = """
                    Authenticates a user using the supplied credentials.
                    On successful authentication, the server establishes
                    an authenticated session.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation failure"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials or authentication failed"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Request rejected by CSRF protection"
            )
    })
    public LoginResponse login(
            @Parameter(description = "User login credentials")
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        return authService.login(
                request,
                httpRequest,
                httpResponse
        );
    }
}