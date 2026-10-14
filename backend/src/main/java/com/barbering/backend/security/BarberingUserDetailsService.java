package com.Barbering.backend.security;

import com.Barbering.backend.entities.User;
import com.Barbering.backend.entities.UserPasswordCredential;
import com.Barbering.backend.repositories.UserPasswordCredentialRepository;
import com.Barbering.backend.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a user (and its password credential) by email for authentication.
 * Email is normalized the same way at signup, so lookups are case-insensitive.
 */
@Service
public class BarberingUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserPasswordCredentialRepository credentialRepository;

    public BarberingUserDetailsService(UserRepository userRepository,
                                       UserPasswordCredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = User.normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown email"));
        UserPasswordCredential credential = credentialRepository.findByUserId(user.getId())
                .orElse(null);
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                credential == null ? "" : credential.getPasswordHash()
        );
    }
}
