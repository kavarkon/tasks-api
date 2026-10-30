package api.packs;

import api.TestContainersBaseTest;
import api.packs.dto.CreatePackRequest;
import api.packs.dto.UpdatePackRequest;
import api.tasks.Task;
import api.tasks.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PackControllerTest extends TestContainersBaseTest {

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Test
    void packIndexReturnsPacksWithoutTasksJson() throws Exception {
        packRepository.saveAll(java.util.List.of(new Pack("pack1"), new Pack("pack2")));

        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("pack1")));
    }

    @Test
    void packCreateReturnsCreatedPack() throws Exception {
        CreatePackRequest request = new CreatePackRequest();
        request.setTitle("pack");

        MvcResult result = mockMvc.perform(post("/api/packs")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("pack")))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
        String location = result.getResponse().getHeader("Location");
        assertEquals("/api/packs/" + id, location);
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.tasks", hasSize(0)));
    }

    @Test
    void packUpdateReturnsUpdatedPack() throws Exception {
        int packId = packRepository.save(new Pack("pack")).getId();
        UpdatePackRequest request = new UpdatePackRequest();
        request.setTitle("updatedPack");

        mockMvc.perform(put("/api/packs/{id}", packId)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("updatedPack")));

        entityManager.flush();
        entityManager.clear();
        assertEquals("updatedPack", packRepository.findById(packId).orElseThrow().getTitle());
    }

    @Test
    void packUpdateReturnsNotFoundForUnknownPack() throws Exception {
        UpdatePackRequest request = new UpdatePackRequest();
        request.setTitle("updatedPack");

        mockMvc.perform(put("/api/packs/{id}", 999)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void packDeleteReturnsNoContentAndDeletesTasks() throws Exception {
        Pack pack = packRepository.save(new Pack("pack"));
        int packId = pack.getId();
        int taskId = taskRepository.save(new Task("task", true, pack)).getId();
        taskRepository.flush();

        mockMvc.perform(delete("/api/packs/{id}", packId))
                .andExpect(status().isNoContent());

        taskRepository.flush();
        entityManager.clear();
        org.junit.jupiter.api.Assertions.assertTrue(taskRepository.findById(taskId).isEmpty());
        assertFalse(packRepository.existsById(packId));
    }

    @Test
    void packDeleteReturnsNotFoundForUnknownPack() throws Exception {
        mockMvc.perform(delete("/api/packs/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void packDetailsContainsOnlyItsTasks() throws Exception {
        Pack pack = packRepository.save(new Pack("first"));
        Pack otherPack = packRepository.save(new Pack("other"));
        Task task = taskRepository.save(new Task("own task", false, pack));
        taskRepository.save(new Task("other task", true, otherPack));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/packs/{id}", pack.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pack.getId())))
                .andExpect(jsonPath("$.title", is("first")))
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].id", is(task.getId())))
                .andExpect(jsonPath("$.tasks[0].title", is("own task")))
                .andExpect(jsonPath("$.tasks[0].completed", is(false)))
                .andExpect(jsonPath("$.tasks[0].packId", is(pack.getId())));
    }

    @Test
    void packDetailsReturnsNotFoundForUnknownPack() throws Exception {
        mockMvc.perform(get("/api/packs/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.path", is("/api/packs/999")));
    }

    @Test
    void packCreateValidatesTitle() throws Exception {
        for (String body : java.util.List.of("{}", "{\"title\":\"  \"}")) {
            mockMvc.perform(post("/api/packs").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }
}
