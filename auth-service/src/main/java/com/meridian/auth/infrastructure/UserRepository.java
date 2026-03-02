package com.meridian.auth.infrastructure;

import com.meridian.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing {@link User} entities.
 * Provides abstracted Data Access Object (DAO) operations targeting the
 * PostgreSQL {@code users} table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Queries the database for a user matching the exact internal username.
     *
     * @param username The exact username to search for.
     * @return An Optional containing the user if found.
     */
    Optional<User> findByUsername(String username);

    /**
     * Queries the database for a user matching the exact email address.
     *
     * @param email The exact email to search for.
     * @return An Optional containing the user if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Performs a highly optimized count query to determine if a username is already
     * taken.
     * Critical for enforcing unique constraints during registration.
     *
     * @param username The username requested for registration.
     * @return {@code true} if the username is taken, {@code false} if it is
     *         available.
     */
    boolean existsByUsername(String username);

    /**
     * Performs a highly optimized count query to determine if an email address is
     * already taken.
     * Critical for enforcing unique constraints during registration.
     *
     * @param email The email requested for registration.
     * @return {@code true} if the email is taken, {@code false} if it is available.
     */
    boolean existsByEmail(String email);
}
