# Auth Service Mapping Guide

This document outlines where to find key implementations and concepts within the `auth-service` module to assist with code reviews and navigation.

## Spring Security

All the hard-coded Spring Security mechanisms (like configuring our route permissions and disabling traditional forms/CSRF) are located in the `infrastructure/` package.

- **`SecurityConfig.java`**: The core configuration class where we declare which endpoints are public (`/api/v1/auth/**`) and wire up our authentication providers.
- **`CustomUserDetailsService.java`**: Evaluates credentials directly from the database to hand off into the security context.
- **`JwtAuthenticationFilter.java`**: The interceptor that grabs the token out of the HTTP `Authorization` header, parses it, and forces Spring Security to acknowledge the user.

## JWT (JSON Web Tokens)

JWT configuration and utility functions are also located in the `infrastructure/` package, with the actual DTOs living in the `presentation/` package.

- **`JwtTokenProvider.java`**: The utility class that actually generates the raw token strings, extracts the claims (username/expiration), and validates signatures against the master secret key.
- **`TokenRefreshRequest.java` & `TokenRefreshResponse.java`** (in `presentation/dto/`): The data models used when a user exchanges their long-lived refresh token for a brand new short-lived access token.

## BCrypt Mapping

- **`SecurityConfig.java`**: This file contains the `@Bean` definition for the `PasswordEncoder()`, returning a fresh `new BCryptPasswordEncoder()`. Spring Security automatically leverages this bean whenever it evaluates passwords.
- **`AuthService.java`** (in `application/`): If you look inside the `registerUser()` method, you will see exactly where we intercept the raw password and encrypt it on the spot using the `passwordEncoder.encode()` method before persisting it to the database.

## PostgreSQL Mapping & JPA Entities

Clean Architecture puts our models inside the `domain/` package, completely isolated from framework logic, while the database hooks exist in `infrastructure/`.

- **`User.java` & `RefreshToken.java`** (in `domain/`): These are the pure data models mapped to PostgreSQL syntax using JPA annotations like `@Entity`, `@Table(name = "users")`, and `@Column(unique = true)`.
- **`UserRepository.java` & `RefreshTokenRepository.java`** (in `infrastructure/`): The Spring Data JPA interfaces that let us execute database queries (like `findByUsername()` or `save()`) without having to write native SQL statements!
