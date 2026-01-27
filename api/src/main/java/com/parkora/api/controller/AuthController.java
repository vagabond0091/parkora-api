package com.parkora.api.controller;

import com.parkora.api.dto.LoginRequest;
import com.parkora.api.dto.common.ApiResponse;
import com.parkora.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @PostMapping(value = "/login", consumes = {"application/json", "multipart/form-data", "application/x-www-form-urlencoded"})
    public ResponseEntity<ApiResponse<String>> authenticateUser(@Valid @ModelAttribute LoginRequest loginRequest) {
        try {
            // Authenticate and get JWT token
            String jwt = authService.authenticate(loginRequest);

            // Build API response
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .message("Login successful")
                    .errorCode(200)
                    .status("SUCCESS")
                    .data(jwt)
                    .build();

            logger.info("User logged in successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            logger.warn("Login failed - invalid credentials for user: {}", loginRequest.getUsername());
            
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .message("Invalid username or password")
                    .errorCode(401)
                    .status("UNAUTHORIZED")
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (UsernameNotFoundException e) {
            logger.warn("Login failed - user not found: {}", loginRequest.getUsername());
            
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .message("Invalid username or password")
                    .errorCode(401)
                    .status("UNAUTHORIZED")
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            logger.error("Login failed with unexpected error", e);
            
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .message("Login failed: " + e.getMessage())
                    .errorCode(500)
                    .status("INTERNAL_ERROR")
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
