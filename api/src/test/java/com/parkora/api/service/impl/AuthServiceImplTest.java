package com.parkora.api.service.impl;

import com.parkora.api.dto.LoginRequest;
import com.parkora.api.entity.Role;
import com.parkora.api.entity.User;
import com.parkora.api.repository.UserRepository;
import com.parkora.api.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest validLoginRequest;
    private User user;
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        // Common setup for all tests
        validLoginRequest = createValidLoginRequest();
        user = createUser(UUID.randomUUID(), "john.doe", "john@example.com");
        authentication = mock(Authentication.class);
    }

    // Helper methods for test data creation
    private LoginRequest createValidLoginRequest() {
        return LoginRequest.builder()
                .username("john.doe")
                .password("password123")
                .build();
    }

    private LoginRequest createLoginRequest(String username, String password) {
        return LoginRequest.builder()
                .username(username)
                .password(password)
                .build();
    }

    private User createUser(UUID id, String username, String email) {
        Role partnersRole = Role.builder()
                .id(UUID.randomUUID())
                .name("PARTNERS")
                .description("Partners role")
                .build();
        
        Set<Role> roles = new HashSet<>();
        roles.add(partnersRole);

        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("$2a$10$encodedPasswordHash")
                .firstName("John")
                .lastName("Doe")
                .status(User.UserStatus.ACTIVE)
                .roles(roles)
                .build();
    }

    private void mockSuccessfulAuthentication(String username) {
        when(authentication.getName()).thenReturn(username);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
    }

    // Positive test cases
    @Test
    public void authenticate_withValidCredentials_shouldReturnJwtToken() {
        // Given
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        mockSuccessfulAuthentication("john.doe");
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(jwtToken);

        // When
        String result = authService.authenticate(validLoginRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(jwtToken);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("john.doe");
        verify(jwtService).generateToken(user);
    }

    @Test
    public void authenticate_shouldCallAuthenticationManagerWithCorrectCredentials() {
        // Given
        String jwtToken = "test-jwt-token";
        mockSuccessfulAuthentication("john.doe");
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(jwtToken);

        // When
        authService.authenticate(validLoginRequest);

        // Then
        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor = 
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        
        UsernamePasswordAuthenticationToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getPrincipal()).isEqualTo("john.doe");
        assertThat(capturedToken.getCredentials()).isEqualTo("password123");
    }

    @Test
    public void authenticate_shouldRetrieveUserFromRepository() {
        // Given
        String jwtToken = "test-jwt-token";
        mockSuccessfulAuthentication("john.doe");
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(jwtToken);

        // When
        authService.authenticate(validLoginRequest);

        // Then
        verify(userRepository).findByUsername("john.doe");
        verify(userRepository, times(1)).findByUsername(anyString());
    }

    @Test
    public void authenticate_shouldGenerateJwtTokenForUser() {
        // Given
        String jwtToken = "test-jwt-token";
        mockSuccessfulAuthentication("john.doe");
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(jwtToken);

        // When
        String result = authService.authenticate(validLoginRequest);

        // Then
        verify(jwtService).generateToken(user);
        assertThat(result).isEqualTo(jwtToken);
    }

    @Test
    public void authenticate_withDifferentUsername_shouldAuthenticateCorrectly() {
        // Given
        LoginRequest loginRequest = createLoginRequest("jane.doe", "password456");
        User janeUser = createUser(UUID.randomUUID(), "jane.doe", "jane@example.com");
        String jwtToken = "jane-jwt-token";
        
        mockSuccessfulAuthentication("jane.doe");
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(janeUser));
        when(jwtService.generateToken(janeUser)).thenReturn(jwtToken);

        // When
        String result = authService.authenticate(loginRequest);

        // Then
        assertThat(result).isEqualTo(jwtToken);
        verify(userRepository).findByUsername("jane.doe");
        verify(jwtService).generateToken(janeUser);
    }

    // Negative test cases
    @Test
    public void authenticate_withInvalidPassword_shouldThrowBadCredentialsException() {
        // Given
        LoginRequest invalidRequest = createLoginRequest("john.doe", "wrongPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(invalidRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    public void authenticate_withNonExistentUsername_shouldThrowBadCredentialsException() {
        // Given
        LoginRequest invalidRequest = createLoginRequest("nonexistent.user", "password123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(invalidRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("User not found");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    public void authenticate_whenUserNotFoundInRepository_shouldThrowUsernameNotFoundException() {
        // Given
        String jwtToken = "test-jwt-token";
        mockSuccessfulAuthentication("john.doe");
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(validLoginRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: john.doe");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("john.doe");
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    public void authenticate_whenAuthenticationFails_shouldNotCallRepositoryOrJwtService() {
        // Given
        LoginRequest invalidRequest = createLoginRequest("john.doe", "wrongPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Authentication failed"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(invalidRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    public void authenticate_shouldUseUsernameFromAuthenticationObject() {
        // Given
        String jwtToken = "test-jwt-token";
        String authenticatedUsername = "authenticated.user";
        User authenticatedUser = createUser(UUID.randomUUID(), authenticatedUsername, "auth@example.com");
        
        when(authentication.getName()).thenReturn(authenticatedUsername);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(authenticatedUser));
        when(jwtService.generateToken(authenticatedUser)).thenReturn(jwtToken);

        // When
        String result = authService.authenticate(validLoginRequest);

        // Then
        assertThat(result).isEqualTo(jwtToken);
        verify(userRepository).findByUsername(authenticatedUsername);
        verify(jwtService).generateToken(authenticatedUser);
    }
}
