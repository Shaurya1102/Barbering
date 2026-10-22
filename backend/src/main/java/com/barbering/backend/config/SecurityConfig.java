package com.Barbering.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Central authentication and authorization (ADR-002): Spring Security with
 * HTTP-only cookie sessions. No JWT, no client-side token storage.
 *
 * CSRF stays enabled: the XSRF-TOKEN cookie is readable by JavaScript and
 * must be echoed back in the X-XSRF-TOKEN header on mutating requests.
 *
 * CORS comes from the single shared {@code CorsConfigurationSource} bean in
 * {@link CorsConfig}; {@code .cors(Customizer.withDefaults())} picks it up.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @org.springframework.beans.factory.annotation.Value("${server.servlet.session.cookie.secure:false}") boolean cookieSecure,
            @org.springframework.beans.factory.annotation.Value("${server.servlet.session.cookie.same-site:lax}") String cookieSameSite) throws Exception {

        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (cookieSecure || "none".equalsIgnoreCase(cookieSameSite)) {
            csrfRepo.setCookieCustomizer(cookie -> {
                if (cookieSecure) cookie.secure(true);
                if ("none".equalsIgnoreCase(cookieSameSite)) cookie.sameSite("None");
            });
        }

        http
                .cors(Customizer.withDefaults())
                // Double-submit-cookie CSRF protection: XSRF-TOKEN cookie is set
                // with HttpOnly=false so the frontend can read it and send it
                // back as the X-XSRF-TOKEN header on POST/PUT/DELETE. The plain
                // request handler compares that header directly against the
                // cookie token (the default XOR-masking handler only makes sense
                // for tokens rendered into HTML, which this JSON API never does).
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                // Defend against session fixation: migrate the context to a new
                // session id when authentication changes.
                .sessionManagement(session -> session.sessionFixation().migrateSession())
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests never carry cookies/CSRF tokens.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public auth endpoints; everything else requires a session.
                        // Public read-only discovery (Google Places + member merge).
                        .requestMatchers(HttpMethod.GET, "/discovery/**").permitAll()
                        .requestMatchers("/auth/signup", "/auth/login", "/auth/csrf", "/barbers/**", "/barber-photos/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied.\"}");
                        }));
        return http.build();
    }
}
