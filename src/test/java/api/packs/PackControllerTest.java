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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

        mockMvc.perform(post("/api/packs")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("pack")));
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
    }

    @Test
    void packDeleteReturnsNotFoundForUnknownPack() throws Exception {
        mockMvc.perform(delete("/api/packs/{id}", 999))
                .andExpect(status().isNotFound());
    }
}
