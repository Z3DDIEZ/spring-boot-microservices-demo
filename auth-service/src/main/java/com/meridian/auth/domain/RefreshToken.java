package com.meridian.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a long-lived Refresh Token issued during successful
 * authentication.
 * <p>
 * Refresh tokens are securely persisted in the database and linked via a 1-to-1
 * physical mapping to the user. They enable clients to obtain new short-lived
 * JWT Access Tokens without prompting the user to re-authenticate, while
 * keeping the system secure through rotation and explicit expiration tracking.
 */
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = "token")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /**
     * The internal primary key for the token record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user to whom this refresh token belongs.
     * Foreign key tied to the {@code users} table.
     */
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    /**
     * The secure, randomly generated token string presented by the client during
     * refresh attempts.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The absolute UTC instant marking when this token is no longer
     * constitutionally valid.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Evaluates if the refresh token has passed its expiration window.
     *
     * @return true if the token is expired, false if it is currently valid.
     */
    public boolean isExpired() {
        return expiryDate.compareTo(Instant.now()) < 0;
    }
}
