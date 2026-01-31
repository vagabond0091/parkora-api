package com.parkora.api.controller;

import com.parkora.api.constant.ApiConstants;
import com.parkora.api.dto.RegisterRequest;
import com.parkora.api.dto.common.ApiResponse;
import com.parkora.api.entity.User;
import com.parkora.api.service.JwtService;
import com.parkora.api.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(ApiConstants.API + ApiConstants.VERSION + ApiConstants.AUTH)
@RequiredArgsConstructor
@Tag(name = "Register", description = "Parkora API for user registration")
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    private final RegisterService registerService;
    private final JwtService jwtService;

    @PostMapping(value = "/register/partners", consumes = {"application/json"})
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Register the user
            User user = registerService.register(registerRequest);

            // Generate JWT token for the newly registered user
            String token = jwtService.generateToken(user);

            // Build API response
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .message("User registered successfully")
                    .code(201)
                    .status("CREATED")
                    .data(token)
                    .build();

            logger.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Handle duplicate username/email
            logger.warn("Registration failed - duplicate entry: {}", e.getMessage());
            
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .message(e.getMessage())
                    .code(409)
                    .status("CONFLICT")
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            // Handle other errors
            logger.error("Registration failed with unexpected error", e);
            
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .message("Registration failed: " + e.getMessage())
                    .code(500)
                    .status("INTERNAL_ERROR")
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
