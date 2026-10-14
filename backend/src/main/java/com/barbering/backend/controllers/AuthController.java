package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.ApiError;
import com.Barbering.backend.dto.LoginRequest;
import com.Barbering.backend.dto.SignUpRequest;
import com.Barbering.backend.dto.UserDto;
import com.Barbering.backend.entities.Role;
import com.Barbering.backend.entities.User;
import com.Barbering.backend.entities.UserPasswordCredential;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.UserPasswordCredentialRepository;
import com.Barbering.backend.repositories.UserRepository;
import com.Barbering.backend.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

/**
 * Email/password authentication endpoints. Sessions are server-side
 * (HttpSession behind an HTTP-only JSESSIONID cookie); the client never
 * stores tokens.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserPasswordCredentialRepository credentialRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final BarberProfileRepository barberProfileRepository;

    public AuthController(UserRepository userRepository,
                          UserPasswordCredentialRepository credentialRepository,
                          AuthenticationManager authenticationManager,
                          PasswordEncoder passwordEncoder,
                          BarberProfileRepository barberProfileRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.barberProfileRepository = barberProfileRepository;
    }

    /**
     * Creates a CUSTOMER account and starts an authenticated session.
     * 201 + UserDto on success, 409 on duplicate email, 400 with field
     * details on invalid input (handled by ApiExceptionHandler).
     */
    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpRequest request,
                                    HttpServletRequest httpRequest) {
        String email = User.normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("An account with this email already exists."));
        }

        Role role = request.getRole() == null ? Role.CUSTOMER : Role.valueOf(request.getRole().toUpperCase());
        User user = new User(email, request.getName().trim(), role);
        userRepository.save(user);
        credentialRepository.save(
                new UserPasswordCredential(user.getId(), passwordEncoder.encode(request.getPassword())));
        if (role == Role.BARBER) {
            barberProfileRepository.save(new BarberProfile(user.getId()));
        }

        // Start a fresh authenticated session and persist the security context
        // so subsequent requests load it (the filter chain only loads).
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        var session = httpRequest.getSession(true);
        startSessionAuthentication(new UserPrincipal(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), true, ""));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
    }

    /**
     * Authenticates by email/password and starts a session. Any credential
     * failure returns the same generic 401 so responses do not reveal whether
     * an account exists.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            Authentication authentication = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            User.normalizeEmail(request.email()), request.password()));
            httpRequest.getSession(true);
            httpRequest.changeSessionId();
            var session = httpRequest.getSession(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(new UserDto(
                    principal.id(), principal.email(), principal.displayName(), principal.role()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Invalid email or password."));
        }
    }

    /**
     * Returns the currently authenticated user. Unauthenticated requests are
     * rejected with a JSON 401 by the security filter chain before reaching
     * this method.
     */
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.id()).map(UserDto::from)
                .orElseGet(() -> new UserDto(principal.id(), principal.email(), principal.displayName(), principal.role()));
    }

    /**
     * Ends the session. The invalidated JSESSIONID is no longer accepted by
     * the server, so the client is signed out even if it still holds the cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<java.util.Map<String, Boolean>> logout(HttpServletRequest httpRequest) {
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    /**
     * Exposes the CSRF token to the SPA (double-submit cookie convention).
     * Resolving the deferred CsrfToken forces the repository to mint the
     * XSRF-TOKEN cookie when one is not present yet.
     */
    @GetMapping("/csrf")
    public java.util.Map<String, String> csrf(CsrfToken token) {
        return java.util.Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    private void startSessionAuthentication(UserPrincipal principal) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
