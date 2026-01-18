package com.parkora.api.service.impl;

import com.parkora.api.dto.RegisterRequest;
import com.parkora.api.entity.Role;
import com.parkora.api.entity.User;
import com.parkora.api.repository.RoleRepository;
import com.parkora.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterServiceImpl registerService;

    private RegisterRequest validRegisterRequest;
    private User savedUser;
    private Role partnersRole;

    @BeforeEach
    public void setUp() {
        // Common setup for all tests
        validRegisterRequest = createValidRegisterRequest();
        savedUser = createUser(UUID.randomUUID(), "john.doe", "john@example.com");
        partnersRole = createRole(UUID.randomUUID(), "PARTNERS");
    }

    // Helper methods for test data creation
    private RegisterRequest createValidRegisterRequest() {
        return RegisterRequest.builder()
                .username("john.doe")
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();
    }

    private RegisterRequest createRegisterRequest(String username, String email, String password) {
        return RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private User createUser(UUID id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .status(User.UserStatus.ACTIVE)
                .build();
    }

    private Role createRole(UUID id, String name) {
        return Role.builder()
                .id(id)
                .name(name)
                .description("Partners role")
                .build();
    }

    private void mockUserDoesNotExist(String username, String email) {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
    }

    private void mockPasswordEncoding(String rawPassword, String encodedPassword) {
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
    }

    // Positive test cases
    @Test
    public void register_withValidRequest_shouldReturnUserWithPartnersRole() {
        // Given
        mockUserDoesNotExist(validRegisterRequest.getUsername(), validRegisterRequest.getEmail());
        mockPasswordEncoding("password123", "encodedPassword123");
        when(roleRepository.findByName("PARTNERS")).thenReturn(Optional.of(partnersRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        User result = registerService.register(validRegisterRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john.doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getPhone()).isEqualTo("1234567890");
        assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles().iterator().next().getName()).isEqualTo("PARTNERS");
        assertThat(result.getPassword()).isEqualTo("encodedPassword123");

        verify(userRepository).existsByUsername("john.doe");
        verify(userRepository).existsByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(roleRepository).findByName("PARTNERS");
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void register_whenPartnersRoleExists_shouldUseExistingRole() {
        // Given
        mockUserDoesNotExist(validRegisterRequest.getUsername(), validRegisterRequest.getEmail());
        mockPasswordEncoding("password123", "encodedPassword123");
        when(roleRepository.findByName("PARTNERS")).thenReturn(Optional.of(partnersRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        User result = registerService.register(validRegisterRequest);

        // Then
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles().iterator().next().getName()).isEqualTo("PARTNERS");
        verify(roleRepository).findByName("PARTNERS");
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    public void register_whenPartnersRoleNotExists_shouldCreateNewRole() {
        // Given
        mockUserDoesNotExist(validRegisterRequest.getUsername(), validRegisterRequest.getEmail());
        mockPasswordEncoding("password123", "encodedPassword123");
        when(roleRepository.findByName("PARTNERS")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return role;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        User result = registerService.register(validRegisterRequest);

        // Then
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles().iterator().next().getName()).isEqualTo("PARTNERS");

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        Role createdRole = roleCaptor.getValue();
        assertThat(createdRole.getName()).isEqualTo("PARTNERS");
        assertThat(createdRole.getDescription()).isEqualTo("Partners role");
    }

    @Test
    public void register_withMinimalRequiredFields_shouldReturnUser() {
        // Given
        RegisterRequest minimalRequest = createRegisterRequest("jane.doe", "jane@example.com", "password123");
        mockUserDoesNotExist("jane.doe", "jane@example.com");
        mockPasswordEncoding("password123", "encodedPassword123");
        when(roleRepository.findByName("PARTNERS")).thenReturn(Optional.of(partnersRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        User result = registerService.register(minimalRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("jane.doe");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        assertThat(result.getRoles()).hasSize(1);
    }

    // Negative test cases
    @Test
    public void register_withDuplicateUsername_shouldThrowIllegalArgumentException() {
        // Given
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registerService.register(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists: " + validRegisterRequest.getUsername());

        verify(userRepository).existsByUsername("john.doe");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    public void register_withDuplicateEmail_shouldThrowIllegalArgumentException() {
        // Given
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registerService.register(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists: " + validRegisterRequest.getEmail());

        verify(userRepository).existsByUsername("john.doe");
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    public void register_withBothDuplicateUsernameAndEmail_shouldThrowIllegalArgumentExceptionForUsername() {
        // Given
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registerService.register(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository).existsByUsername("john.doe");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    public void register_shouldEncodePasswordBeforeSaving() {
        // Given
        mockUserDoesNotExist(validRegisterRequest.getUsername(), validRegisterRequest.getEmail());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPasswordHash");
        when(roleRepository.findByName("PARTNERS")).thenReturn(Optional.of(partnersRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        User result = registerService.register(validRegisterRequest);

        // Then
        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPasswordHash");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void register_shouldSetUserStatusToActive() {
        // Given
        mockUserDoesNotExist(validRegisterRequest.getUsername(), validRegisterRequest.getEmail());
        mockPasswordEncoding("password123", "encodedPassword123");
        when(roleRepository.findByName("PARTNERS")).thenReturn(Optional.of(partnersRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        User result = registerService.register(validRegisterRequest);

        // Then
        assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }
}
