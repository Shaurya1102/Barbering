package com.Barbering.backend.dto;

import com.Barbering.backend.entities.Role;
import com.Barbering.backend.entities.User;

/**
 * The only user representation returned to clients. No password hash, no
 * internal state — safe for the browser.
 */
public record UserDto(Long id, String email, String displayName, Role role) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
