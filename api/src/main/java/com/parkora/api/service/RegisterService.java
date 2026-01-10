package com.parkora.api.service;

import com.parkora.api.dto.RegisterRequest;
import com.parkora.api.entity.User;

public interface RegisterService {
    /**
     * Register a new user
     * 
     * @param registerRequest the registration request containing user details
     * @return the created user entity
     * @throws IllegalArgumentException if username or email already exists
     */
    User register(RegisterRequest registerRequest);
}
