package api.packs;

import api.TestContainersBaseTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static api.ApiTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PackTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    private String accessToken;
    private static final String PACKS = "/api/packs";

    @BeforeEach
    void createAuthenticatedUser() throws Exception {
        accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
    }

    @Test
    void listReturnsOnlyCurrentUserPacks() throws Exception {
        int packId = createPack(mockMvc, accessToken, "Own pack");
        String otherToken = createUserAndGetAccessToken(mockMvc, "other@example.com");
        createPack(mockMvc, otherToken, "Other pack");
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("[{\"id\":%d,\"title\":\"Own pack\"}]".formatted(packId), STRICT));
    }

    @Test
    void listReturnsEmptyArrayWhenUserHasNoPacks() throws Exception {
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("[]", STRICT));
    }

    @Test
    void getReturnsPackWithOnlyItsTasksSortedById() throws Exception {
        int packId = createPack(mockMvc, accessToken, "Study");
        int firstId = createTask(mockMvc, accessToken, "Z", false, packId);
        int secondId = createTask(mockMvc, accessToken, "A", true, packId);
        int otherId = createPack(mockMvc, accessToken, "Other");
        createTask(mockMvc, accessToken, "Excluded", false, otherId);
        mockMvc.perform(get(PACKS + "/" + packId).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":%d,"title":"Study","tasks":[
                          {"id":%d,"title":"Z","completed":false,"packId":%d},
                          {"id":%d,"title":"A","completed":true,"packId":%d}]}
                        """.formatted(packId, firstId, packId, secondId, packId), STRICT));
    }

    @Test
    void getReturnsEmptyTasksArrayForPackWithoutTasks() throws Exception {
        int packId = createPack(mockMvc, accessToken, "Empty");
        mockMvc.perform(get(PACKS + "/" + packId).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("{\"id\":%d,\"title\":\"Empty\",\"tasks\":[]}".formatted(packId), STRICT));
    }

    @Test
    void createReturnsPackAndLocationWithAuthenticatedOwner() throws Exception {
        String otherToken = createUserAndGetAccessToken(mockMvc, "other@example.com");
        Integer otherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, "other@example.com");
        var result = mockMvc.perform(createChangingRequest(mockMvc, "POST", PACKS, accessToken)
                        .contentType(APPLICATION_JSON).content(encodeJson(Map.of("title", "Mine", "userId", otherId))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Mine")).andReturn();
        String path = PACKS + "/" + readCreatedId(result, PACKS);
        mockMvc.perform(get(path).cookie(accessCookie(accessToken))).andExpect(status().isOk());
        mockMvc.perform(get(path).cookie(accessCookie(otherToken))).andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsAndPersistsUpdatedPack() throws Exception {
        int packId = createPack(mockMvc, accessToken, "Before");
        mockMvc.perform(createChangingRequest(mockMvc, "PUT", PACKS + "/" + packId, accessToken)
                        .contentType(APPLICATION_JSON).content("{\"title\":\"After\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(packId))
                .andExpect(jsonPath("$.title").value("After"));
        assertStoredTitle(packId, "After");
    }

    @Test
    void deleteRemovesPackAndItsTasks() throws Exception {
        int packId = createPack(mockMvc, accessToken, "Delete");
        int taskId = createTask(mockMvc, accessToken, "Cascade", false, packId);
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(createChangingRequest(mockMvc, "DELETE", PACKS + "/" + packId, accessToken))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        entityManager.flush();
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM packs WHERE id=?", Integer.class, packId));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM tasks WHERE id=?", Integer.class, taskId));
    }

    @ParameterizedTest
    @MethodSource("api.ApiTestSupport#provideUnavailableResources")
    void getUpdateAndDeleteReturnNotFoundForMissingOrOtherUserPack(String method, String ownership) throws Exception {
        boolean foreign = ownership.equals("foreign");
        int packId = foreign ? createPack(mockMvc, createUserAndGetAccessToken(mockMvc, "other@example.com"), "Private") : -1;
        String path = PACKS + "/" + packId;
        var request = method.equals("GET") ? get(path).cookie(accessCookie(accessToken))
                : createChangingRequest(mockMvc, method, path, accessToken);
        if (method.equals("PUT")) request.contentType(APPLICATION_JSON).content("{\"title\":\"Changed\"}");
        mockMvc.perform(request).andExpect(status().isNotFound());
        if (foreign) assertStoredTitle(packId, "Private");
    }

    @ParameterizedTest
    @MethodSource("api.ApiTestSupport#provideInvalidTitles")
    void createAndUpdateRejectInvalidTitle(String method, String title) throws Exception {
        int packId = createPack(mockMvc, accessToken, "Original");
        String path = method.equals("POST") ? PACKS : PACKS + "/" + packId;
        mockMvc.perform(createChangingRequest(mockMvc, method, path, accessToken).contentType(APPLICATION_JSON)
                        .content(title == null ? "{}" : encodeJson(Map.of("title", title))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").isNotEmpty());
        assertStoredTitle(packId, "Original");
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM packs", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void createAndUpdateAcceptTitleWith255Characters(String method) throws Exception {
        String title = "x".repeat(255);
        boolean creating = method.equals("POST");
        int packId = creating ? -1 : createPack(mockMvc, accessToken, "Before");
        String path = creating ? PACKS : PACKS + "/" + packId;
        var result = mockMvc.perform(createChangingRequest(mockMvc, method, path, accessToken)
                        .contentType(APPLICATION_JSON).content(encodeJson(Map.of("title", title))))
                .andExpect(creating ? status().isCreated() : status().isOk()).andExpect(jsonPath("$.title").value(title)).andReturn();
        assertStoredTitle(creating ? readCreatedId(result, PACKS) : packId, title);
    }

    private void assertStoredTitle(int packId, String title) {
        entityManager.flush();
        assertEquals(title, jdbcTemplate.queryForObject("SELECT title FROM packs WHERE id=?", String.class, packId));
    }
}
