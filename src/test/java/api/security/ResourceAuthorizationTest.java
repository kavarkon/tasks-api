package api.security;

import api.TestContainersBaseTest;
import api.users.User;
import api.users.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import static api.ApiTestSupport.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceAuthorizationTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Value("${jwt.secret}") private String signingSecret;

    static Stream<Arguments> provideProtectedRoutes() {
        return Stream.of("/api/packs", "/api/tasks").flatMap(path -> Stream.of(
                Arguments.of("GET", path), Arguments.of("GET", path + "/1"), Arguments.of("POST", path),
                Arguments.of("PUT", path + "/1"), Arguments.of("DELETE", path + "/1")));
    }

    @ParameterizedTest
    @MethodSource("provideProtectedRoutes")
    void rejectProtectedRequestsWithoutAccessToken(String method, String path) throws Exception {
        assertUnauthorized(method, path, null);
    }

    @ParameterizedTest
    @MethodSource("provideProtectedRoutes")
    void rejectProtectedRequestsWithMalformedAccessToken(String method, String path) throws Exception {
        assertUnauthorized(method, path, "not-a-jwt");
    }

    @ParameterizedTest
    @MethodSource("provideProtectedRoutes")
    void rejectProtectedRequestsWithExpiredAccessToken(String method, String path) throws Exception {
        User user = userRepository.saveAndFlush(new User("existing@example.com", "unused"));
        Instant now = Instant.now();
        String expiredToken = Jwts.builder().subject(user.getId().toString())
                .issuedAt(Date.from(now.minusSeconds(120))).expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        assertUnauthorized(method, path, expiredToken);
    }

    private void assertUnauthorized(String method, String path, String token) throws Exception {
        var request = method.equals("GET") ? get(path) : createChangingRequest(method, path, null);
        if (token != null) request.cookie(accessCookie(token));
        if (method.equals("POST") || method.equals("PUT")) {
            String body = path.startsWith("/api/packs") ? "{\"title\":\"Pack\"}"
                    : "{\"title\":\"Task\",\"completed\":false,\"packId\":1}";
            request.contentType(APPLICATION_JSON).content(body);
        }
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }
}
