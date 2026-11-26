package api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgreSQLTestConfiguration.class)
class ApiHttpTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @LocalServerPort private int port;
    private HttpClient client;

    @BeforeEach
    void createClient() {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        client = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void registerLoginCreateReadTaskAndLogoutWithoutCsrfOverHttp() throws Exception {
        var credentials = Map.of("email", "http-user@example.com", "password", "example-password");
        assertEquals(201, postJson("/api/auth/register", credentials).statusCode());
        assertEquals(401, get("/api/packs").statusCode());

        assertEquals(200, postJson("/api/auth/login", credentials).statusCode());
        var createdPack = postJson("/api/packs", Map.of("title", "Study"));
        assertEquals(201, createdPack.statusCode());
        int packId = JSON.readTree(createdPack.body()).get("id").intValue();
        var task = Map.of("title", "Read chapter", "completed", false, "packId", packId);
        var createdTask = postJson("/api/tasks", task);
        assertEquals(201, createdTask.statusCode());
        int taskId = JSON.readTree(createdTask.body()).get("id").intValue();
        String location = createdTask.headers().firstValue(LOCATION).orElseThrow();
        assertEquals("/api/tasks/" + taskId, location);

        var storedTask = get(location);
        assertEquals(200, storedTask.statusCode());
        var expectedTask = Map.of("id", taskId, "title", "Read chapter", "completed", false, "packId", packId);
        assertEquals(JSON.valueToTree(expectedTask), JSON.readTree(storedTask.body()));

        var logout = createRequest("/api/auth/logout").POST(HttpRequest.BodyPublishers.noBody());
        assertEquals(204, send(logout).statusCode());
        assertEquals(401, get(location).statusCode());
    }

    @Test
    void allowPreflightFromConfiguredOrigin() throws Exception {
        String origin = "http://localhost:5173";
        var response = sendPreflight(origin);
        assertEquals(200, response.statusCode());
        assertEquals(origin, response.headers().firstValue(ACCESS_CONTROL_ALLOW_ORIGIN).orElseThrow());
        assertEquals("true", response.headers().firstValue(ACCESS_CONTROL_ALLOW_CREDENTIALS).orElseThrow());
        assertTrue(readHeaderTokens(response, ACCESS_CONTROL_ALLOW_METHODS).contains("post"));
        assertEquals(Set.of("content-type"), readHeaderTokens(response, ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    void rejectPreflightFromUnconfiguredOrigin() throws Exception {
        var response = sendPreflight("https://other.example");
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().allValues(ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty());
    }

    private HttpResponse<String> postJson(String path, Map<String, ?> fields)
            throws IOException, InterruptedException {
        return send(createRequest(path).header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(fields))));
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(createRequest(path).GET());
    }

    private HttpResponse<String> sendPreflight(String origin) throws IOException, InterruptedException {
        return send(createRequest("/api/tasks").header(ORIGIN, origin)
                .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
    }

    private Set<String> readHeaderTokens(HttpResponse<String> response, String headerName) {
        return response.headers().allValues(headerName).stream().flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::strip).map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private HttpRequest.Builder createRequest(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
