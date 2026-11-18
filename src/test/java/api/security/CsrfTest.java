package api.security;

import api.TestContainersBaseTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.stream.Stream;

import static api.ApiTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CsrfTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void getCsrfTokenWithoutAuthenticationReturnsCookieAndHeaderDetails() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN")).andExpect(jsonPath("$.token").isNotEmpty());
    }

    static Stream<Arguments> provideRejectedRequests() {
        return Stream.of("POST /api/auth/register", "POST /api/auth/login", "POST /api/auth/logout",
                        "POST /api/packs", "PUT /api/packs/{id}", "DELETE /api/packs/{id}",
                        "POST /api/tasks", "PUT /api/tasks/{id}", "DELETE /api/tasks/{id}")
                .flatMap(route -> Stream.of("missing", "invalid").map(token -> Arguments.of(route, token)));
    }

    @ParameterizedTest
    @MethodSource("provideRejectedRequests")
    void rejectStateChangingRequestsWithMissingOrInvalidCsrfToken(String route, String tokenState) throws Exception {
        String owner = "owner@example.com";
        String accessToken = createUserAndGetAccessToken(mockMvc, owner);
        int packId = createPack(mockMvc, accessToken, "Original pack");
        int taskId = createTask(mockMvc, accessToken, "Original task", false, packId);
        entityManager.flush();
        entityManager.clear();
        String[] parts = route.split(" ");
        String path = parts[1].replace("{id}", String.valueOf(route.contains("/tasks") ? taskId : packId));
        var request = request(HttpMethod.valueOf(parts[0]), path);
        if (!path.equals("/api/auth/register") && !path.equals("/api/auth/login")) request.cookie(accessCookie(accessToken));
        if (!parts[0].equals("DELETE") && !path.endsWith("/logout")) {
            String body = path.contains("/packs") ? encodeJson(Map.of("title", "Changed pack"))
                    : path.contains("/tasks") ? encodeJson(Map.of("title", "Changed task", "completed", true, "packId", packId))
                    : createCredentials(path.endsWith("/register") ? "new@example.com" : owner);
            request.contentType(APPLICATION_JSON).content(body);
        }
        if (tokenState.equals("invalid")) {
            var csrfToken = fetchCsrfToken(mockMvc);
            request.cookie(csrfToken.cookie()).header(csrfToken.header(), "invalid-token");
        }
        mockMvc.perform(request).andExpect(status().isForbidden()).andExpect(cookie().doesNotExist("access_token"));
        entityManager.flush();
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM packs", Integer.class));
        assertEquals("Original pack", jdbcTemplate.queryForObject("SELECT title FROM packs WHERE id=?", String.class, packId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM tasks", Integer.class));
        assertEquals(Map.of("title", "Original task", "completed", false, "pack_id", packId),
                jdbcTemplate.queryForMap("SELECT title,completed,pack_id FROM tasks WHERE id=?", taskId));
    }

    @Test
    void getFreshCsrfTokenAfterLoginAndLogout() throws Exception {
        String credentials = createCredentials("owner@example.com");
        var initialToken = fetchCsrfToken(mockMvc);
        mockMvc.perform(initialToken.applyTo(post("/api/auth/register")).contentType(APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        var loginResponse = mockMvc.perform(initialToken.applyTo(post("/api/auth/login"))
                        .contentType(APPLICATION_JSON).content(credentials)).andExpect(status().isOk())
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0)).andReturn().getResponse();
        var accessCookie = loginResponse.getCookie("access_token");
        var afterLogin = fetchCsrfToken(mockMvc, accessCookie);
        assertNotEquals(initialToken.cookie().getValue(), afterLogin.cookie().getValue());
        mockMvc.perform(afterLogin.applyTo(post("/api/auth/logout").cookie(accessCookie)))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge("XSRF-TOKEN", 0));
        var afterLogout = fetchCsrfToken(mockMvc);
        assertNotEquals(afterLogin.cookie().getValue(), afterLogout.cookie().getValue());
        mockMvc.perform(afterLogout.applyTo(post("/api/auth/login")).contentType(APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk());
    }
}
