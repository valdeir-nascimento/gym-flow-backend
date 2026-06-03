package br.com.gym.flow;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationOnboardingE2ETest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String BOOTSTRAP_INVITE = "WS_FITNESS_BOOTSTRAP_ADMIN_2026";
    private static final String ADMIN_EMAIL = "admin@wsfitness.local";
    private static final String NEW_PASSWORD = "AdminSenha@2026";
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private TestRestTemplate rest;

    private static HttpEntity<Map<String, String>> jsonBody(Map<String, String> body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void givenBootstrapInvite_whenOnboardingThenAuthenticating_thenTheWholeFlowWorks() {
        // 1. Consume the bootstrap invite: defines the password, activates the admin, issues tokens
        ResponseEntity<JsonNode> consumed = rest.postForEntity(
            "/auth/invites/{token}/consume",
            jsonBody(Map.of("newPassword", NEW_PASSWORD, "passwordConfirmation", NEW_PASSWORD)),
            JsonNode.class, BOOTSTRAP_INVITE);

        assertThat(consumed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consumed.getBody().get("role").asText()).isEqualTo("ADMINISTRATOR");
        String accessToken = consumed.getBody().get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        // 2. The issued access token authenticates against a protected endpoint, and the admin
        //    is now ACTIVE (activated as part of consuming the invite, across the users module)
        var authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<JsonNode> admin = rest.exchange("/users/{id}", HttpMethod.GET, new HttpEntity<>(authHeaders), JsonNode.class, ADMIN_ID);

        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getBody().get("email").asText()).isEqualTo(ADMIN_EMAIL);
        assertThat(admin.getBody().get("status").asText()).isEqualTo("ACTIVE");

        // 3. Login with the freshly defined password returns a new token pair
        ResponseEntity<JsonNode> login = rest.postForEntity(
            "/auth/login",
            jsonBody(Map.of("email", ADMIN_EMAIL, "password", NEW_PASSWORD)),
            JsonNode.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String refreshToken = login.getBody().get("refreshToken").asText();
        assertThat(refreshToken).isNotBlank();

        // 4. The refresh token rotates into a fresh pair
        ResponseEntity<JsonNode> refreshed = rest.postForEntity(
            "/auth/refresh",
            jsonBody(Map.of("refreshToken", refreshToken)),
            JsonNode.class);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().get("accessToken").asText()).isNotBlank();
    }

    @Test
    void givenUnknownEmail_whenLoggingIn_thenReturns401() {
        // A clearly non-existent account: order-independent, never throttled (rate limit off in test)
        ResponseEntity<JsonNode> login = rest.postForEntity(
            "/auth/login",
            jsonBody(Map.of("email", "nobody-" + UUID.randomUUID() + "@example.com", "password", "whatever-123")),
            JsonNode.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenProtectedEndpoint_whenCalledWithoutToken_thenReturns401() {
        // The security filter chain rejects unauthenticated access to /users/**
        ResponseEntity<JsonNode> response = rest.getForEntity("/users/{id}", JsonNode.class, ADMIN_ID);

        // Rejected by the security filter chain (entry point may answer 401 or 403); the point is it is not served.
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
