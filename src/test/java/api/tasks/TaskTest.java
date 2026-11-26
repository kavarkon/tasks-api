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
import java.util.List;
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
    void listTasksFromAllOwnedPacksAcrossPages() throws Exception {
        int first = createTask(mockMvc, accessToken, "Z", false, packId);
        String otherToken = createUserAndGetAccessToken(mockMvc, "other@example.com");
        createTask(mockMvc, otherToken, "Private", true, createPack(mockMvc, otherToken, "Private"));
        int secondPack = createPack(mockMvc, accessToken, "Other");
        int second = createTask(mockMvc, accessToken, "A", true, secondPack);

        var firstPage = mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("limit", "1"))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[%s],"nextAfterId":%d}
                        """.formatted(expectedTask(first, "Z", false, packId), first), STRICT)).andReturn();
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("limit", "1")
                        .param("afterId", readNextAfterId(firstPage)))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[%s],"nextAfterId":null}
                        """.formatted(expectedTask(second, "A", true, secondPack)), STRICT));
    }

    @Test
    void listTasksFromSelectedPackAcrossPages() throws Exception {
        int first = createTask(mockMvc, accessToken, "Z", false, packId);
        createTask(mockMvc, accessToken, "Excluded", false, createPack(mockMvc, accessToken, "Other"));
        int second = createTask(mockMvc, accessToken, "A", true, packId);

        var firstPage = mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("limit", "1")
                        .param("packId", String.valueOf(packId)))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[%s],"nextAfterId":%d}
                        """.formatted(expectedTask(first, "Z", false, packId), first), STRICT)).andReturn();
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("limit", "1")
                        .param("afterId", readNextAfterId(firstPage)).param("packId", String.valueOf(packId)))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[%s],"nextAfterId":null}
                        """.formatted(expectedTask(second, "A", true, packId)), STRICT));
    }

    @Test
    void listReturnsEmptyPageWhenUserHasNoTasks() throws Exception {
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("limit", "100")).andExpect(status().isOk())
                .andExpect(content().json(EMPTY_PAGE, STRICT));
    }

    @Test
    void listReturnsEmptyPageForEmptyOwnedPack() throws Exception {
        createTask(mockMvc, accessToken, "Other", false, createPack(mockMvc, accessToken, "Other"));
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("packId", String.valueOf(packId)))
                .andExpect(status().isOk()).andExpect(content().json(EMPTY_PAGE, STRICT));
    }

    @Test
    void listTasksReturnsNotFoundForForeignOrDeletedPack() throws Exception {
        int foreign = createPack(mockMvc, createUserAndGetAccessToken(mockMvc, "other@example.com"), "Private");
        mockMvc.perform(createChangingRequest("DELETE", "/api/packs/" + packId, accessToken))
                .andExpect(status().isNoContent());
        for (int unavailable : List.of(foreign, packId)) {
            mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("packId", String.valueOf(unavailable)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        }
    }

    @ParameterizedTest
    @MethodSource("api.ApiTestSupport#provideInvalidPaginationParameters")
    void listTasksRejectsInvalidPaginationParameter(String name, String value) throws Exception {
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param(name, value))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "2147483648", "", "   "})
    void listTasksRejectsInvalidPackFilter(String value) throws Exception {
        mockMvc.perform(get(TASKS).cookie(accessCookie(accessToken)).param("packId", value))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getReturnsCurrentUserTask() throws Exception {
        int taskId = createTask(mockMvc, accessToken, "Read", false, packId);
        assertStoredTask(taskId, "Read", false, packId);
    }

    @Test
    void createReturnsTaskAndLocation() throws Exception {
        var result = mockMvc.perform(createChangingRequest("POST", TASKS, accessToken)
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
        mockMvc.perform(createChangingRequest("PUT", TASKS + "/" + taskId, accessToken)
                        .contentType(APPLICATION_JSON).content(encodeJson(taskFields(title, completed, destination))))
                .andExpect(status().isOk()).andExpect(content().json(expectedTask(taskId, title, completed, destination), STRICT));
        assertStoredTask(taskId, title, completed, destination);
    }

    @Test
    void deleteRemovesTask() throws Exception {
        int taskId = createTask(mockMvc, accessToken, "Delete", false, packId);
        mockMvc.perform(createChangingRequest("DELETE", TASKS + "/" + taskId, accessToken))
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
                : createChangingRequest(method, path, accessToken);
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

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void createAndUpdateRejectExplicitNullCompleted(String method) throws Exception {
        var fields = new HashMap<>(taskFields("Changed", false, packId));
        fields.put("completed", null);
        rejectTaskWrite(method, fields, "completed");
    }

    private void rejectTaskWrite(String method, Map<String, Object> fields, String invalidField) throws Exception {
        int originalId = createTask(mockMvc, accessToken, "Original", false, packId);
        String path = method.equals("POST") ? TASKS : TASKS + "/" + originalId;
        var result = mockMvc.perform(createChangingRequest(method, path, accessToken)
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
