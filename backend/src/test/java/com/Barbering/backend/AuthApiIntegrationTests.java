package com.Barbering.backend;

import com.Barbering.backend.entities.Role;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.entities.User;
import com.Barbering.backend.entities.UserPasswordCredential;
import com.Barbering.backend.repositories.UserPasswordCredentialRepository;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.containsString;

/**
 * End-to-end tests for the auth API and role-based dashboard authorization.
 * Runs against the H2 in-memory database configured in src/test/resources/application.properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPasswordCredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BarberProfileRepository barberProfileRepository;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    /** Signs up a fresh customer and returns the session that now holds its authentication. */
    private MockHttpSession signUp(String name, String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/auth/signup")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"%s"}"""
                                        .formatted(name, email, password)))
                .andExpect(status().isCreated());
        return session;
    }

    /** Logs in with existing credentials and returns the authenticated session. */
    private MockHttpSession login(String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/auth/login")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}"""
                                        .formatted(email, password)))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void signupCreatesCustomerAndStartsSession() throws Exception {
        String email = uniqueEmail();

        MockHttpSession session = signUp("Ada Lovelace", email, "password123");

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void signupWithDuplicateEmailReturns409() throws Exception {
        String email = uniqueEmail();
        signUp("First User", email, "password123");

        mockMvc.perform(post("/auth/signup")
                        .session(new MockHttpSession())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second User","email":"%s","password":"password456"}"""
                                        .formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("An account with this email already exists."));
    }

    @Test
    void signupWithInvalidInputReturns400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .session(new MockHttpSession())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed."))
                .andExpect(jsonPath("$.details[?(@ == 'Name is required.')]").exists())
                .andExpect(jsonPath("$.details[?(@ == 'Email must be a valid address.')]").exists())
                .andExpect(jsonPath("$.details[?(@ == 'Password must be between 8 and 72 characters.')]").exists());
    }

    @Test
    void loginWithValidCredentialsReturnsUserAndStartsSession() throws Exception {
        String email = uniqueEmail();
        signUp("Grace Hopper", email, "password123");

        MockHttpSession session = login(email, "password123");

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void loginWithWrongPasswordReturnsGeneric401() throws Exception {
        String email = uniqueEmail();
        signUp("Grace Hopper", email, "password123");

        mockMvc.perform(post("/auth/login")
                        .session(new MockHttpSession())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}"""
                                        .formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    void loginWithUnknownEmailReturnsSameGeneric401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .session(new MockHttpSession())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"whatever123"}"""
                                        .formatted(uniqueEmail())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    void meWithoutSessionReturnsJson401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = signUp("Alan Turing", email, "password123");

        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanAccessClientDashboard() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = signUp("Client Customer", email, "password123");

        mockMvc.perform(get("/dashboard/client").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Client dashboard"))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void customerCannotAccessBarberDashboard() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = signUp("Regular Customer", email, "password123");

        mockMvc.perform(get("/dashboard/barber").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied."));
    }

    @Test
    void barberCanLoginAndAccessBarberDashboard() throws Exception {
        String email = uniqueEmail();
        User barber = userRepository.save(new User(email, "Barber Bob", Role.BARBER));
        barberProfileRepository.save(new BarberProfile(barber.getId()));
        credentialRepository.save(
                new UserPasswordCredential(barber.getId(), passwordEncoder.encode("barberpass1")));

        MockHttpSession session = login(email, "barberpass1");

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BARBER"));

        mockMvc.perform(get("/dashboard/barber").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Barber dashboard"))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void csrfEndpointMintsCookieAndAllowsSignup() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/csrf")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        JsonNode body = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        assertThat(body.get("token").asText()).isEqualTo(cookie.getValue());
        assertThat(body.get("headerName").asText()).isEqualTo("X-XSRF-TOKEN");
        String email = uniqueEmail();
        mockMvc.perform(post("/auth/signup")
                        .session(new MockHttpSession())
                        .cookie(cookie)
                        .header(body.get("headerName").asText(), body.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Csrf Client","email":"%s","password":"password123"}"""
                                        .formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void signupWithoutCsrfHeaderIsRejectedWith403() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"No Token","email":"%s","password":"password123"}"""
                                        .formatted(uniqueEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied."));
    }

    @Test
    void credentialedCorsPreflightAllowsPatch() throws Exception {
        mockMvc.perform(options("/appointments/42/cancel")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }
}
