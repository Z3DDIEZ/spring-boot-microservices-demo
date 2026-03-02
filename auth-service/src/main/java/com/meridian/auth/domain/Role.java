package com.meridian.auth.domain;

/**
 * Defines the standard authorization roles available within the Meridian
 * platform.
 * Embedded within JWT claims to facilitate stateless RBAC (Role-Based Access
 * Control)
 * down the line.
 */
public enum Role {
    /**
     * Standard user role implicitly granted upon public registration.
     */
    ROLE_USER,

    /**
     * Privileged administrative role intended for internal estate operations.
     */
    ROLE_ADMIN
}
