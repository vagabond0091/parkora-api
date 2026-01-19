package com.parkora.api.service;

import com.parkora.api.dto.LoginRequest;

public interface AuthService {
    /**
     * Authenticate a user and generate JWT token
     * 
     * @param loginRequest the login request containing username and password
     * @return the JWT token string
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if user is not found
     */
    String authenticate(LoginRequest loginRequest);
}
