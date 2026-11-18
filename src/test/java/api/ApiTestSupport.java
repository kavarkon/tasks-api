package api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ApiTestSupport {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ApiTestSupport() {}

    public record CsrfToken(Cookie cookie, String header, String value) {
        public MockHttpServletRequestBuilder applyTo(MockHttpServletRequestBuilder request) {
            return request.cookie(cookie).header(header, value);
        }
    }

    public static CsrfToken fetchCsrfToken(MockMvc mockMvc, Cookie... cookies) throws Exception {
        var request = get("/api/auth/csrf");
        if (cookies.length > 0) request.cookie(cookies);
        var response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
        var body = JSON.readTree(response.getContentAsByteArray());
        return new CsrfToken(response.getCookie("XSRF-TOKEN"), body.get("headerName").stringValue(),
                body.get("token").stringValue());
    }

    public static Cookie accessCookie(String token) {
        return new Cookie("access_token", token);
    }

    public static MockHttpServletRequestBuilder createChangingRequest(MockMvc mockMvc, String method,
            String path, String accessToken) throws Exception {
        var request = request(HttpMethod.valueOf(method), path);
        if (accessToken != null) request.cookie(accessCookie(accessToken));
        return fetchCsrfToken(mockMvc).applyTo(request);
    }

    public static String encodeJson(Map<String, ?> fields) {
        return JSON.writeValueAsString(fields);
    }

    public static String createCredentials(String email) {
        return encodeJson(Map.of("email", email, "password", "example-password"));
    }

    public static String createUserAndGetAccessToken(MockMvc mockMvc, String email) throws Exception {
        String credentials = createCredentials(email);
        mockMvc.perform(createChangingRequest(mockMvc, "POST", "/api/auth/register", null)
                        .contentType(APPLICATION_JSON).content(credentials)).andExpect(status().isCreated());
        return mockMvc.perform(createChangingRequest(mockMvc, "POST", "/api/auth/login", null)
                        .contentType(APPLICATION_JSON).content(credentials)).andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token").getValue();
    }

    public static int readCreatedId(MvcResult result, String collectionPath) {
        int id = JSON.readTree(result.getResponse().getContentAsByteArray()).get("id").intValue();
        assertEquals(collectionPath + "/" + id, result.getResponse().getHeader("Location"));
        return id;
    }

    public static int createPack(MockMvc mockMvc, String token, String title) throws Exception {
        var result = mockMvc.perform(createChangingRequest(mockMvc, "POST", "/api/packs", token)
                        .contentType(APPLICATION_JSON).content(encodeJson(Map.of("title", title))))
                .andExpect(status().isCreated()).andReturn();
        return readCreatedId(result, "/api/packs");
    }

    public static int createTask(MockMvc mockMvc, String token, String title, boolean completed, int packId)
            throws Exception {
        var result = mockMvc.perform(createChangingRequest(mockMvc, "POST", "/api/tasks", token)
                        .contentType(APPLICATION_JSON)
                        .content(encodeJson(Map.of("title", title, "completed", completed, "packId", packId))))
                .andExpect(status().isCreated()).andReturn();
        return readCreatedId(result, "/api/tasks");
    }

    public static Stream<Arguments> provideInvalidTitles() {
        return Stream.of("POST", "PUT").flatMap(method -> Stream.of(
                Arguments.of(method, null), Arguments.of(method, ""), Arguments.of(method, "   "),
                Arguments.of(method, "x".repeat(256))));
    }

    public static Stream<Arguments> provideUnavailableResources() {
        return Stream.of("GET", "PUT", "DELETE").flatMap(method -> Stream.of(
                Arguments.of(method, "missing"), Arguments.of(method, "foreign")));
    }
}
