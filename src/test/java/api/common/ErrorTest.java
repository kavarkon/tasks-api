package api.common;

import api.TestContainersBaseTest;
import api.packs.PackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static api.ApiTestSupport.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private PackService packService;

    @ParameterizedTest
    @ValueSource(strings = {"/api/packs/abc", "/api/tasks/abc", "/api/packs/2147483648", "/api/tasks/2147483648"})
    void returnBadRequestForNonNumericOrOutOfRangePathId(String path) throws Exception {
        String accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        mockMvc.perform(get(path).cookie(accessCookie(accessToken))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"/api/packs | {", "/api/packs | {\"title\":123}",
            "/api/tasks | {\"title\":\"Task\",\"completed\":\"true\",\"packId\":1}",
            "/api/tasks | {\"title\":\"Task\",\"completed\":false,\"packId\":\"1\"}"})
    void returnBadRequestForMalformedJsonOrInvalidFieldType(String path, String body) throws Exception {
        String accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        mockMvc.perform(createChangingRequest(mockMvc, "POST", path, accessToken).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.path").value(path));
    }

    @Test
    void returnGeneralErrorWithStatusMessageAndPath() throws Exception {
        String accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        mockMvc.perform(get("/api/tasks/42").cookie(accessCookie(accessToken))).andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"status":404,"message":"Task with id 42 was not found","path":"/api/tasks/42"}
                        """, STRICT));
    }

    @Test
    void returnValidationErrorsWithFieldMessages() throws Exception {
        mockMvc.perform(createChangingRequest(mockMvc, "POST", "/api/auth/register", null)
                        .contentType(APPLICATION_JSON).content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().json("""
                        {"status":400,"message":"Проверьте введённые данные","path":"/api/auth/register",
                         "fields":{"email":"Некорректный адрес электронной почты","password":"Пароль обязателен"}}
                        """, STRICT));
    }

    @Test
    void returnInternalServerErrorWithoutTechnicalDetails() throws Exception {
        when(packService.index(any())).thenThrow(new IllegalStateException("unexpected database detail"));
        String accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        mockMvc.perform(get("/api/packs").cookie(accessCookie(accessToken))).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"status\":500,\"message\":\"Internal server error\",\"path\":\"/api/packs\"}", STRICT));
    }
}
