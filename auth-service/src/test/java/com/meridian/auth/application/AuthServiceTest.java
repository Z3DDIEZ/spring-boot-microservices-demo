package com.meridian.auth.application;

import com.meridian.auth.domain.Role;
import com.meridian.auth.domain.User;
import com.meridian.auth.infrastructure.JwtTokenProvider;
import com.meridian.auth.infrastructure.RefreshTokenRepository;
import com.meridian.auth.infrastructure.UserRepository;
import com.meridian.auth.presentation.dto.AuthResponse;
import com.meridian.auth.presentation.dto.LoginRequest;
import com.meridian.auth.presentation.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                tokenProvider,
                3600000L
        );
    }

    @Test
    void registerUser_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@ex.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@ex.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");

        // Act
        authService.registerUser(request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@ex.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded_pass");
        assertThat(savedUser.getRoles()).contains(Role.ROLE_USER);
        assertThat(savedUser.isActive()).isTrue();
    }

    @Test
    void registerUser_UsernameTaken_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.registerUser(request));
        
        assertThat(exception.getMessage()).contains("Username is already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_EmailTaken_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@ex.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@ex.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.registerUser(request));
        
        assertThat(exception.getMessage()).contains("Email is already in use");
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateUser_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                "testuser", "password123", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateToken(auth)).thenReturn("mock_jwt");

        User user = new User();
        user.setUsername("testuser");
        user.setRoles(Set.of(Role.ROLE_USER));
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        com.meridian.auth.domain.RefreshToken token = new com.meridian.auth.domain.RefreshToken();
        token.setToken("mock_refresh");
        when(refreshTokenRepository.save(any())).thenReturn(token);

        AuthResponse res = authService.authenticateUser(request);

        assertThat(res.getAccessToken()).isEqualTo("mock_jwt");
        assertThat(res.getRefreshToken()).isEqualTo("mock_refresh");
        assertThat(res.getUsername()).isEqualTo("testuser");
    }

    @Test
    void authenticateUser_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                "testuser", "password123", List.of());
        
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.authenticateUser(request));
        
        assertThat(exception.getMessage()).isEqualTo("User not found");
    }

}
