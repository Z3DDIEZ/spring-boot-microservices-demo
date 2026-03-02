package com.meridian.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The core domain entity representing a registered user within the Meridian
 * ecosystem.
 * <p>
 * This entity is managed by the {@code auth-service} and mapped to the
 * {@code "users"} table.
 * It strictly enforces unique constraints on both the username and email to
 * prevent
 * duplicate registrations. Roles are eagerly fetched to avoid
 * LazyInitializationExceptions
 * during JWT token generation.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    /**
     * The unique, system-generated identifier for the user.
     * Serves as the primary key and the principal 'sub' claim in JWTs.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * The user's chosen display name or login handle. Unique across the system.
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * The user's email address. Unique across the system and utilized for
     * notifications.
     */
    @Column(nullable = false, length = 100)
    private String email;

    /**
     * The securely BCrypt-hashed representation of the user's password.
     * Never store or log plaintext passwords.
     */
    @Column(nullable = false)
    private String password;

    /**
     * The set of authorization roles granted to this user.
     * Eagerly fetched to ensure availability during authentication flows.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Soft-delete or suspension flag. Determines if the account can successfully
     * authenticate.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Audit timestamp marking when the user record was initially successfully
     * persisted.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit timestamp marking the most recent modification to the user record.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
