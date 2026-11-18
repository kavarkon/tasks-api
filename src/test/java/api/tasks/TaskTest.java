package api.tasks;

import api.TestContainersBaseTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static api.ApiTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskTest extends TestContainersBaseTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    private static final String TASKS = "/api/tasks";
    private String accessToken;
    private int packId;

    @BeforeEach
    void createAuthenticatedUserAndPack() throws Exception {
        accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
        packId = createPack(mockMvc, accessToken, "Default");
    }

    @Test
    void listReturnsOnlyCurrentUserTasks() throws Exception {
        int ownTaskId = createTask(mockMvc, accessToken, "Mine", false, packId);
        String otherToken = createUserAndGetAccessToken(mockMvc, "other@example.com");
        createTask(mockMvc, otherToken, "Private", true, createPack(mockMvc, otherToken, "Other"));
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("[" + expectedTask(ownTaskId, "Mine", false, packId) + "]", STRICT));
    }

    @Test
    void listReturnsEmptyArrayWhenUserHasNoTasks() throws Exception {
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("[]", STRICT));
    }

    @Test
    void getReturnsCurrentUserTask() throws Exception {
        int taskId = createTask(mockMvc, accessToken, "Read", false, packId);
        assertStoredTask(taskId, "Read", false, packId);
    }

    @Test
    void createReturnsTaskAndLocation() throws Exception {
        var result = mockMvc.perform(createChangingRequest(mockMvc, "POST", TASKS, accessToken)
                        .contentType(APPLICATION_JSON).content(encodeJson(taskFields("Read", false, packId))))
                .andExpect(status().isCreated()).andReturn();
        int taskId = readCreatedId(result, TASKS);
        content().json(expectedTask(taskId, "Read", false, packId), STRICT).match(result);
        assertStoredTask(taskId, "Read", false, packId);
    }

    @Test
    void updateReturnsAndPersistsUpdatedTask() throws Exception {
        int taskId = createTask(mockMvc, accessToken, "Before", false, packId);
        updateAndCheckTask(taskId, "After", true, packId);
    }

    @Test
    void updateMovesTaskToAnotherOwnedPack() throws Exception {
        int taskId = createTask(mockMvc, accessToken, "Move", false, packId);
        int destination = createPack(mockMvc, accessToken, "Destination");
        updateAndCheckTask(taskId, "Move", false, destination);
    }

    private void updateAndCheckTask(int taskId, String title, boolean completed, int destination) throws Exception {
        mockMvc.perform(createChangingRequest(mockMvc, "PUT", TASKS + "/" + taskId, accessToken)
                        .contentType(APPLICATION_JSON).content(encodeJson(taskFields(title, completed, destination))))
                .andExpect(status().isOk()).andExpect(content().json(expectedTask(taskId, title, completed, destination), STRICT));
        assertStoredTask(taskId, title, completed, destination);
    }

    @Test
    void deleteRemovesTask() throws Exception {
        int taskId = createTask(mockMvc, accessToken, "Delete", false, packId);
        mockMvc.perform(createChangingRequest(mockMvc, "DELETE", TASKS + "/" + taskId, accessToken))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        entityManager.flush();
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM tasks WHERE id=?", Integer.class, taskId));
    }

    @ParameterizedTest
    @MethodSource("api.ApiTestSupport#provideUnavailableResources")
    void getUpdateAndDeleteReturnNotFoundForMissingOrOtherUserTask(String method, String ownership) throws Exception {
        String otherToken = createUserAndGetAccessToken(mockMvc, "other@example.com");
        int otherPackId = createPack(mockMvc, otherToken, "Other");
        int taskId = ownership.equals("foreign") ? createTask(mockMvc, otherToken, "Private", false, otherPackId) : -1;
        String path = TASKS + "/" + taskId;
        var request = method.equals("GET") ? get(path).cookie(accessCookie(accessToken))
                : createChangingRequest(mockMvc, method, path, accessToken);
        if (method.equals("PUT")) request.contentType(APPLICATION_JSON).content(encodeJson(taskFields("Changed", true, packId)));
        mockMvc.perform(request).andExpect(status().isNotFound());
        if (ownership.equals("foreign")) {
            entityManager.flush();
            entityManager.clear();
            mockMvc.perform(get(path).cookie(accessCookie(otherToken))).andExpect(status().isOk())
                    .andExpect(content().json(expectedTask(taskId, "Private", false, otherPackId), STRICT));
        }
    }

    @ParameterizedTest
    @CsvSource({"POST,missing", "POST,foreign", "PUT,missing", "PUT,foreign"})
    void createAndUpdateReturnNotFoundForMissingOrOtherUserPack(String method, String ownership) throws Exception {
        int target = ownership.equals("foreign")
                ? createPack(mockMvc, createUserAndGetAccessToken(mockMvc, "other@example.com"), "Private") : -1;
        rejectTaskWrite(method, taskFields("Changed", true, target), null);
    }

    @ParameterizedTest
    @MethodSource("api.ApiTestSupport#provideInvalidTitles")
    void createAndUpdateRejectInvalidTitle(String method, String title) throws Exception {
        var fields = new HashMap<>(taskFields("Changed", true, packId));
        if (title == null) fields.remove("title"); else fields.put("title", title);
        rejectTaskWrite(method, fields, "title");
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void createAndUpdateAcceptTitleWith255Characters(String method) throws Exception {
        String title = "x".repeat(255);
        if (method.equals("POST")) {
            int taskId = createTask(mockMvc, accessToken, title, false, packId);
            assertStoredTask(taskId, title, false, packId);
        } else {
            updateAndCheckTask(createTask(mockMvc, accessToken, "Before", false, packId), title, true, packId);
        }
    }

    @ParameterizedTest
    @CsvSource({"POST,completed", "POST,packId", "PUT,completed", "PUT,packId"})
    void createAndUpdateRejectMissingCompletedOrPackId(String method, String missingField) throws Exception {
        var fields = new HashMap<>(taskFields("Changed", true, packId));
        fields.remove(missingField);
        rejectTaskWrite(method, fields, missingField);
    }

    private void rejectTaskWrite(String method, Map<String, Object> fields, String invalidField) throws Exception {
        int originalId = createTask(mockMvc, accessToken, "Original", false, packId);
        String path = method.equals("POST") ? TASKS : TASKS + "/" + originalId;
        var result = mockMvc.perform(createChangingRequest(mockMvc, method, path, accessToken)
                .contentType(APPLICATION_JSON).content(encodeJson(fields)));
        if (invalidField == null) result.andExpect(status().isNotFound());
        else result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields." + invalidField).isNotEmpty());
        assertStoredTask(originalId, "Original", false, packId);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM tasks", Integer.class));
    }

    private void assertStoredTask(int taskId, String title, boolean completed, int destination) throws Exception {
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get(TASKS + "/" + taskId).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json(expectedTask(taskId, title, completed, destination), STRICT));
    }

    private Map<String, Object> taskFields(String title, boolean completed, int destination) {
        return Map.of("title", title, "completed", completed, "packId", destination);
    }

    private String expectedTask(int taskId, String title, boolean completed, int destination) {
        var fields = new HashMap<>(taskFields(title, completed, destination));
        fields.put("id", taskId);
        return encodeJson(fields);
    }
}
