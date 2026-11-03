package com.teamactiontracker.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "Authentication, session management and CSRF protection"
)
public class CsrfController {

    @GetMapping("/csrf")
    @Operation(
            summary = "Get CSRF token",
            description = """
                    Returns the CSRF token associated with the current request.
                    The client should retain the token and send it using the
                    configured CSRF request header for protected state-changing requests.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSRF token retrieved successfully"
            )
    })
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {

        return new CsrfTokenResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        );
    }

    public record CsrfTokenResponse(
            String token,
            String headerName,
            String parameterName
    ) {}
}