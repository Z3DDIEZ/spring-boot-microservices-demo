package com.meridian.auth.infrastructure;

import com.meridian.auth.domain.RefreshToken;
import com.meridian.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing {@link RefreshToken} entities.
 * Facilitates token lookups during the refresh flow and targeted deletions upon
 * logout.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Retrieves a refresh token record by its exact token string.
     *
     * @param token The secure random token string presented by the client.
     * @return An Optional containing the token if found, or empty if it does not
     *         exist.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Hard deletes all refresh tokens associated with a specific user.
     * Typically invoked during account suspension, forced global logout, or when a
     * token is maliciously rotated.
     *
     * @param user The owner of the refresh tokens.
     * @return The number of records deleted.
     */
    Long deleteByUser(User user);
}
