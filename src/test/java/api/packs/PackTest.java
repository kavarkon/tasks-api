package api.packs;

import api.TestContainersBaseTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static api.ApiTestSupport.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PackTest extends TestContainersBaseTest {
    private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    private EntityManager entityManager;
    private String accessToken;
    private static final String PACKS = "/api/packs";

    @BeforeEach
    void createAuthenticatedUser(@Autowired MockMvc mockMvc, @Autowired EntityManager entityManager) throws Exception {
        this.mockMvc = mockMvc;
        this.entityManager = entityManager;
        accessToken = createUserAndGetAccessToken(mockMvc, "owner@example.com");
    }

    @Test
    void listPacksAcrossPagesWithoutRepeatingOrSkippingItems() throws Exception {
        int first = createPack(mockMvc, accessToken, "Z");
        String otherToken = createUserAndGetAccessToken(mockMvc, "other@example.com");
        createPack(mockMvc, otherToken, "Foreign");
        int second = createPack(mockMvc, accessToken, "M");
        int third = createPack(mockMvc, accessToken, "C");
        int fourth = createPack(mockMvc, accessToken, "A");

        var firstPage = mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken)).param("limit", "2"))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[{"id":%d,"title":"Z"},{"id":%d,"title":"M"}],"nextAfterId":%d}
                        """.formatted(first, second, second), STRICT)).andReturn();
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken))
                        .param("limit", "2").param("afterId", readNextAfterId(firstPage)))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[{"id":%d,"title":"C"},{"id":%d,"title":"A"}],"nextAfterId":null}
                        """.formatted(third, fourth), STRICT));
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken)).param("afterId", String.valueOf(fourth)))
                .andExpect(status().isOk()).andExpect(content().json(EMPTY_PAGE, STRICT));
    }

    @Test
    void listPacksWithoutLimitReturnsFiftyItemsThenRemainingPack() throws Exception {
        var packIds = new ArrayList<Integer>();
        for (int index = 0; index < 51; index++) {
            packIds.add(createPack(mockMvc, accessToken, "Pack " + index));
        }
        var firstPage = mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(50)))
                .andExpect(jsonPath("$.nextAfterId").value(packIds.get(49))).andReturn();
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken)).param("afterId", readNextAfterId(firstPage)))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"items":[{"id":%d,"title":"Pack 50"}],"nextAfterId":null}
                        """.formatted(packIds.getLast()), STRICT));
    }

    @Test
    void listReturnsEmptyPageWhenUserHasNoPacks() throws Exception {
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken)).param("limit", "100")).andExpect(status().isOk())
                .andExpect(content().json(EMPTY_PAGE, STRICT));
    }

    @ParameterizedTest
    @MethodSource("api.ApiTestSupport#provideInvalidPaginationParameters")
    void listPacksRejectsInvalidPaginationParameter(String name, String value) throws Exception {
        mockMvc.perform(get(PACKS).cookie(accessCookie(accessToken)).param(name, value))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getPackReturnsOnlyIdAndTitle() throws Exception {
        int packId = createPack(mockMvc, accessToken, "Study");
        createTask(mockMvc, accessToken, "Read", false, packId);
        mockMvc.perform(get(PACKS + "/" + packId).cookie(accessCookie(accessToken))).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":%d,"title":"Study"}
                        """.formatted(packId), STRICT));
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

    @Nested
    class PackDeletion {
        @Test
        void deletePackRemovesItsTasksAndKeepsTasksInOtherPack() throws Exception {
            int studyPack = createPack(mockMvc, accessToken, "Study");
            int firstTask = createTask(mockMvc, accessToken, "Read", false, studyPack);
            int secondTask = createTask(mockMvc, accessToken, "Write", true, studyPack);
            int homePack = createPack(mockMvc, accessToken, "Home");
            int homeTask = createTask(mockMvc, accessToken, "Cook", false, homePack);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(createChangingRequest(mockMvc, "DELETE", PACKS + "/" + studyPack, accessToken))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get(PACKS + "/" + studyPack).cookie(accessCookie(accessToken)))
                    .andExpect(status().isNotFound());
            for (int deletedTask : List.of(firstTask, secondTask)) {
                mockMvc.perform(get("/api/tasks/" + deletedTask).cookie(accessCookie(accessToken)))
                        .andExpect(status().isNotFound());
            }
            mockMvc.perform(get(PACKS + "/" + homePack).cookie(accessCookie(accessToken)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(homePack));
            mockMvc.perform(get("/api/tasks/" + homeTask).cookie(accessCookie(accessToken)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.packId").value(homePack));
        }
    }

    @Nested
    @ActiveProfiles(profiles = "dev", inheritProfiles = false)
    @TestPropertySource(properties = "DATABASE_URL=jdbc:postgresql://unused.invalid/tasks")
    class DevelopmentPackDeletion extends PackDeletion {}

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
