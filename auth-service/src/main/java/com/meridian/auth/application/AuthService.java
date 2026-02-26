package com.meridian.auth.application;

import com.meridian.auth.domain.RefreshToken;
import com.meridian.auth.domain.Role;
import com.meridian.auth.domain.User;
import com.meridian.auth.infrastructure.JwtTokenProvider;
import com.meridian.auth.infrastructure.RefreshTokenRepository;
import com.meridian.auth.infrastructure.UserRepository;
import com.meridian.auth.presentation.dto.AuthResponse;
import com.meridian.auth.presentation.dto.LoginRequest;
import com.meridian.auth.presentation.dto.RegisterRequest;
import com.meridian.auth.presentation.dto.TokenRefreshResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenDurationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       @Value("${jwt.refresh-expiration}") long refreshTokenDurationMs) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    @Transactional
    public void registerUser(RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .roles(Set.of(Role.ROLE_USER))
                .active(true)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String jwt = tokenProvider.generateToken(authentication);
        
        org.springframework.security.core.userdetails.User principal = 
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = createRefreshToken(user);

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .roles(roles)
                .tokenType("Bearer")
                .build();
    }

    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user); // rotation: delete old

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public TokenRefreshResponse refreshToken(String requestRefreshToken) {
        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(token -> {
                    if (token.isExpired()) {
                        refreshTokenRepository.delete(token);
                        throw new RuntimeException("Refresh token was expired. Please make a new signin request");
                    }
                    return token;
                })
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = tokenProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of())); 
                    // Note: usually we'd pass authorities properly, let's fix that
                    List<GrantedAuthority> authorities = user.getRoles().stream()
                            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.name()))
                            .collect(Collectors.toList());
                    
                    org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
                    String newAccessToken = tokenProvider.generateToken(new UsernamePasswordAuthenticationToken(principal, null, authorities));
                    
                    return new TokenRefreshResponse(newAccessToken, requestRefreshToken);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
