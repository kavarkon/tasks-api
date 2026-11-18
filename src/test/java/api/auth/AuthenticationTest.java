package api.auth;

import api.TestContainersBaseTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static api.ApiTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN = "/api/auth/login";
    private static final String LOGOUT = "/api/auth/logout";

    @Test
    void registerReturnsCreatedUserWithoutLoggingIn() throws Exception {
        submit(REGISTER, createCredentials("person@example.com")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.email").value("person@example.com"))
                .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(cookie().doesNotExist("access_token"));
        mockMvc.perform(get("/api/packs")).andExpect(status().isUnauthorized());
    }

    static Stream<Arguments> provideInvalidLoginFields() {
        return Stream.of(Arguments.of("email", null), Arguments.of("email", ""), Arguments.of("email", "invalid"),
                Arguments.of("email", "a".repeat(250) + "@example.com"), Arguments.of("password", null),
                Arguments.of("password", ""), Arguments.of("password", "   "));
    }

    static Stream<Arguments> provideInvalidRegistrationFields() {
        return Stream.concat(provideInvalidLoginFields(), Stream.of(Arguments.of("password", "short"),
                Arguments.of("password", "я".repeat(37))));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidRegistrationFields")
    void registerRejectsInvalidEmailAndPassword(String field, String invalidValue) throws Exception {
        assertInvalidField(REGISTER, field, invalidValue);
    }

    @ParameterizedTest
    @ValueSource(strings = {"case@example.com", "CASE@example.com"})
    void registerRejectsExistingEmailIgnoringCase(String duplicate) throws Exception {
        submit(REGISTER, createCredentials("case@example.com")).andExpect(status().isCreated());
        submit(REGISTER, createCredentials(duplicate)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.path").value(REGISTER))
                .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.fields.email").isNotEmpty());
    }

    @Test
    void rejectDuplicateEmailAtDatabaseConstraint() throws Exception {
        submit(REGISTER, createCredentials("unique@example.com")).andExpect(status().isCreated());
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try (var statement = connection.prepareStatement("INSERT INTO users (email, password_hash) VALUES (?, ?)")) {
                statement.setString(1, "UNIQUE@example.com");
                statement.setString(2, "password-hash");
                assertEquals("23505", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
            } finally {
                connection.rollback(savepoint);
            }
            return null;
        });
    }

    @Test
    void loginReturnsUserAndHttpOnlyAccessTokenCookie() throws Exception {
        String credentials = createCredentials("login@example.com");
        String registeredUser = submit(REGISTER, credentials).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var response = submit(LOGIN, credentials).andExpect(status().isOk())
                .andExpect(content().json(registeredUser, org.springframework.test.json.JsonCompareMode.STRICT))
                .andExpect(cookie().httpOnly("access_token", true)).andReturn().getResponse();
        mockMvc.perform(get("/api/packs").cookie(response.getCookie("access_token"))).andExpect(status().isOk());
    }

    @Nested
    @ActiveProfiles(profiles = "prod", inheritProfiles = false)
    @TestPropertySource(properties = {"JWT_SECRET=test-production-profile-secret-at-least-32-bytes",
            "CORS_ALLOWED_ORIGIN=https://frontend.example.test", "DATABASE_URL=jdbc:postgresql://unused.invalid/tasks"})
    class Production extends TestContainersBaseTest {
        @Autowired private MockMvc productionMockMvc;

        @Test
        void loginSetsSecureAndSameSiteCookieInProduction() throws Exception {
            String credentials = createCredentials("production@example.com");
            productionMockMvc.perform(createChangingRequest(productionMockMvc, "POST", REGISTER, null)
                            .contentType(APPLICATION_JSON).content(credentials)).andExpect(status().isCreated());
            productionMockMvc.perform(createChangingRequest(productionMockMvc, "POST", LOGIN, null)
                            .contentType(APPLICATION_JSON).content(credentials)).andExpect(status().isOk())
                    .andExpect(cookie().secure("access_token", true)).andExpect(cookie().httpOnly("access_token", true))
                    .andExpect(cookie().sameSite("access_token", "Lax"));
        }
    }

    @ParameterizedTest
    @CsvSource({"missing@example.com,example-password", "credentials@example.com,wrong-password"})
    void loginRejectsIncorrectEmailOrPasswordWithSameMessage(String email, String password) throws Exception {
        submit(REGISTER, createCredentials("credentials@example.com")).andExpect(status().isCreated());
        submit(LOGIN, encodeJson(Map.of("email", email, "password", password))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Неверная почта или пароль")).andExpect(cookie().doesNotExist("access_token"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidLoginFields")
    void loginRejectsInvalidOrMissingFields(String field, String invalidValue) throws Exception {
        assertInvalidField(LOGIN, field, invalidValue);
    }

    @Test
    void logoutClearsCookieAndAllowsRepeatedLogout() throws Exception {
        String token = createUserAndGetAccessToken(mockMvc, "logout@example.com");
        mockMvc.perform(createChangingRequest(mockMvc, "POST", LOGOUT, token)).andExpect(status().isNoContent())
                .andExpect(content().string("")).andExpect(cookie().value("access_token", "")).andExpect(cookie().maxAge("access_token", 0));
        mockMvc.perform(createChangingRequest(mockMvc, "POST", LOGOUT, null)).andExpect(status().isNoContent());
    }

    @Test
    void logoutDoesNotInvalidateCopiedJwt() throws Exception {
        String token = createUserAndGetAccessToken(mockMvc, "copied@example.com");
        mockMvc.perform(createChangingRequest(mockMvc, "POST", LOGOUT, token)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/packs").cookie(accessCookie(token))).andExpect(status().isOk());
    }

    private ResultActions submit(String path, String body) throws Exception {
        return mockMvc.perform(createChangingRequest(mockMvc, "POST", path, null).contentType(APPLICATION_JSON).content(body));
    }

    private void assertInvalidField(String path, String field, String invalidValue) throws Exception {
        var fields = new HashMap<>(Map.of("email", "person@example.com", "password", "example-password"));
        if (invalidValue == null) fields.remove(field); else fields.put(field, invalidValue);
        submit(path, encodeJson(fields)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields." + field).isNotEmpty());
    }
}
