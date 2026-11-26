package api.security;

import api.TestContainersBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static api.ApiTestSupport.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CsrfTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void registerLoginAndLogoutWithoutCsrfToken() throws Exception {
        String email = "owner@example.com";
        String credentials = createCredentials(email);
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(cookie().doesNotExist("access_token")).andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        var loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(cookie().exists("access_token")).andExpect(cookie().doesNotExist("XSRF-TOKEN"))
                .andReturn().getResponse();
        mockMvc.perform(post("/api/auth/logout").cookie(loginResponse.getCookie("access_token")))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(cookie().value("access_token", "")).andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(cookie().value("access_token", "")).andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
    }

    @Test
    void createUpdateAndDeletePackWithoutCsrfToken() throws Exception {
        String accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        String collectionPath = "/api/packs";
        String originalTitle = "Original pack";
        var createdPack = mockMvc.perform(post(collectionPath).cookie(accessCookie(accessToken))
                        .contentType(APPLICATION_JSON).content(encodeJson(Map.of("title", originalTitle))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value(originalTitle))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN")).andReturn();
        int packIdentifier = readCreatedId(createdPack, collectionPath);
        String packPath = collectionPath + "/" + packIdentifier;
        String updatedTitle = "Changed pack";
        mockMvc.perform(put(packPath).cookie(accessCookie(accessToken))
                        .contentType(APPLICATION_JSON).content(encodeJson(Map.of("title", updatedTitle))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(packIdentifier))
                .andExpect(jsonPath("$.title").value(updatedTitle)).andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mockMvc.perform(delete(packPath).cookie(accessCookie(accessToken)))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mockMvc.perform(get(packPath).cookie(accessCookie(accessToken)))
                .andExpect(status().isNotFound()).andExpect(cookie().doesNotExist("XSRF-TOKEN"));
    }

    @Test
    void createUpdateAndDeleteTaskWithoutCsrfToken() throws Exception {
        String accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        int packIdentifier = createPack(mockMvc, accessToken, "Original pack");
        String collectionPath = "/api/tasks";
        String originalTitle = "Original task";
        var originalFields = Map.of("title", originalTitle, "completed", false, "packId", packIdentifier);
        var createdTask = mockMvc.perform(post(collectionPath).cookie(accessCookie(accessToken))
                        .contentType(APPLICATION_JSON).content(encodeJson(originalFields)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value(originalTitle))
                .andExpect(jsonPath("$.completed").value(false)).andExpect(jsonPath("$.packId").value(packIdentifier))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN")).andReturn();
        int taskIdentifier = readCreatedId(createdTask, collectionPath);
        String taskPath = collectionPath + "/" + taskIdentifier;
        String updatedTitle = "Changed task";
        var updatedFields = Map.of("title", updatedTitle, "completed", true, "packId", packIdentifier);
        mockMvc.perform(put(taskPath).cookie(accessCookie(accessToken))
                        .contentType(APPLICATION_JSON).content(encodeJson(updatedFields)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(taskIdentifier))
                .andExpect(jsonPath("$.title").value(updatedTitle)).andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.packId").value(packIdentifier)).andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mockMvc.perform(delete(taskPath).cookie(accessCookie(accessToken)))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mockMvc.perform(get(taskPath).cookie(accessCookie(accessToken)))
                .andExpect(status().isNotFound()).andExpect(cookie().doesNotExist("XSRF-TOKEN"));
    }
}
