package api.tasks;

import api.TestContainersBaseTest;
import api.packs.Pack;
import api.packs.PackRepository;
import api.tasks.dto.CreateTaskRequest;
import api.tasks.dto.UpdateTaskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

        mockMvc.perform(post("/api/tasks")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("task")))
                .andExpect(jsonPath("$.packId", is(packId)));
    }

    @Test
    void taskUpdateReturnsUpdatedTask() throws Exception {
        Pack pack = packRepository.findById(packId).orElseThrow();
        int taskId = taskRepository.save(new Task("task", false, pack)).getId();

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("updatedTask");
        request.setCompleted(true);
        request.setPackId(packId);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("updatedTask")))
                .andExpect(jsonPath("$.completed", is(true)));
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
}
