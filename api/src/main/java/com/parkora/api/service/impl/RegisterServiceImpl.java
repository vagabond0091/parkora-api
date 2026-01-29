package com.parkora.api.service.impl;

import com.parkora.api.dto.RegisterRequest;
import com.parkora.api.entity.Role;
import com.parkora.api.entity.User;
import com.parkora.api.repository.RoleRepository;
import com.parkora.api.repository.UserRepository;
import com.parkora.api.service.RegisterService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + registerRequest.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + registerRequest.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phone(registerRequest.getPhone())
                .status(User.UserStatus.ACTIVE)
                .build();

        // Create or get PARTNERS role
        Set<Role> roles = new HashSet<>();
        Role partnersRole = roleRepository.findByName("tests")
                .orElseGet(() -> {
                    // Create PARTNERS role if it doesn't exist
                    Role newRole = Role.builder()
                            .name("tests")
                            .description("Partners role")
                            .build();
                    return roleRepository.save(newRole);
                });
        
        roles.add(partnersRole);
        user.setRoles(roles);

        // Save and return the user
        return userRepository.save(user);
    }
}
