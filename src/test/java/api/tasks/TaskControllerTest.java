package api.tasks;

import api.TestContainersBaseTest;
import api.packs.Pack;
import api.packs.PackRepository;
import api.tasks.dto.CreateTaskRequest;
import api.tasks.dto.UpdateTaskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TaskControllerTest extends TestContainersBaseTest {

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

    private int packId;

    @BeforeEach
    void addPack() {
        Pack pack = packRepository.save(new Pack("pack"));
        packId = pack.getId();
    }

    @Test
    void taskIndexReturnsTasksWithoutRecursivePackJson() throws Exception {
        taskRepository.saveAll(java.util.List.of(
                new Task("task1", true, packRepository.findById(packId).orElseThrow()),
                new Task("task2", false, packRepository.findById(packId).orElseThrow())
        ));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].packId", is(packId)));
    }

    @Test
    void taskCreateReturnsCreatedTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("task");
        request.setCompleted(true);
        request.setPackId(packId);

        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("task")))
                .andExpect(jsonPath("$.packId", is(packId)))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
        String location = result.getResponse().getHeader("Location");
        assertEquals("/api/tasks/" + id, location);
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.title", is("task")))
                .andExpect(jsonPath("$.completed", is(true)))
                .andExpect(jsonPath("$.packId", is(packId)));
    }

    @Test
    void taskUpdateReturnsUpdatedTask() throws Exception {
        Pack pack = packRepository.findById(packId).orElseThrow();
        int taskId = taskRepository.save(new Task("task", false, pack)).getId();
        int otherPackId = packRepository.save(new Pack("other pack")).getId();

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("updatedTask");
        request.setCompleted(true);
        request.setPackId(otherPackId);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("updatedTask")))
                .andExpect(jsonPath("$.completed", is(true)))
                .andExpect(jsonPath("$.packId", is(otherPackId)));

        entityManager.flush();
        entityManager.clear();
        Task updated = taskRepository.findById(taskId).orElseThrow();
        assertEquals("updatedTask", updated.getTitle());
        assertEquals(true, updated.getCompleted());
        assertEquals(otherPackId, updated.getPack().getId());
    }

    @Test
    void taskUpdateReturnsNotFoundForUnknownTask() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("updatedTask");
        request.setCompleted(false);
        request.setPackId(packId);

        mockMvc.perform(put("/api/tasks/{id}", 999)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskDeleteReturnsNoContent() throws Exception {
        Pack pack = packRepository.findById(packId).orElseThrow();
        int taskId = taskRepository.save(new Task("task", true, pack)).getId();

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        assertFalse(taskRepository.existsById(taskId));
    }

    @Test
    void taskDeleteReturnsNotFoundForUnknownTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskCreateValidatesRequest() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(" ");

        mockMvc.perform(post("/api/tasks")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskDetailsReturnsNotFoundForUnknownTask() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.path", is("/api/tasks/999")));
    }

    @Test
    void createAndUpdateRejectUnknownPack() throws Exception {
        CreateTaskRequest create = new CreateTaskRequest();
        create.setTitle("task");
        create.setCompleted(false);
        create.setPackId(999);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isNotFound());

        int taskId = taskRepository.save(new Task("original", false,
                packRepository.findById(packId).orElseThrow())).getId();
        UpdateTaskRequest update = new UpdateTaskRequest();
        update.setTitle("changed");
        update.setCompleted(true);
        update.setPackId(999);
        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskCreateRejectsEachMissingRequiredField() throws Exception {
        for (String body : java.util.List.of(
                "{\"completed\":false,\"packId\":" + packId + "}",
                "{\"title\":\"task\",\"packId\":" + packId + "}",
                "{\"title\":\"task\",\"completed\":false}")) {
            mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void invalidIdReturnsBadRequestWithErrorDetails() throws Exception {
        mockMvc.perform(get("/api/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid value for parameter 'id'")))
                .andExpect(jsonPath("$.path", is("/api/tasks/abc")));
    }

    @Test
    void invalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Request body is invalid")))
                .andExpect(jsonPath("$.path", is("/api/tasks")));
    }
}
