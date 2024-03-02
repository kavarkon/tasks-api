package api.tasks.controller;

import api.tasks.TestContainersBaseTest;
import api.tasks.model.Task;
import api.tasks.model.Pack;
import api.tasks.repository.PackRepository;
import api.tasks.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.hamcrest.CoreMatchers;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.List;

@AutoConfigureMockMvc
class TaskControllerTest extends TestContainersBaseTest {
    @Autowired
    private PackRepository packRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    private int packId1;
    @BeforeEach
    public void addPacks() {
        packRepository.saveAll(List.of(new Pack("pack1"), new Pack("pack2")));
        Iterator<Pack> iterator = packRepository.findAll().iterator();
        packId1 = iterator.next().getId();
    }

    @Test
    public void taskIndexTest() throws Exception {
        List<Task> tasks =
                List.of(new Task("task1", true, packId1),
                        new Task("task2", true, packId1),
                        new Task("task3", true, packId1),
                        new Task("task4", false, packId1));
        taskRepository.saveAll(tasks);

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/tasks"));

        response.andExpect(MockMvcResultMatchers.status().isOk());
        response.andExpect(MockMvcResultMatchers.jsonPath("$.size()", CoreMatchers.is(tasks.size())));
    }
    @Test
    public void taskCreateTest() throws Exception {
        Task task = new Task("task", true, packId1);

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .post("/api/tasks")
                .content(objectMapper.writeValueAsString(task))
	            .contentType(MediaType.APPLICATION_JSON));

        response.andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void  taskUpdateTest() throws Exception {
        int taskId = taskRepository.save(new Task("task", false, packId1)).getId();

        Task task = new Task("updatedTask", false, packId1);
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .put(String.format("/api/tasks/%s", taskId))
                .content(objectMapper.writeValueAsString(task))
	            .contentType(MediaType.APPLICATION_JSON));

        response.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void  taskNotUpdateTest() throws Exception {
        Task task = new Task("updatedTask", false, packId1);

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .put("/api/tasks/2")
                .content(objectMapper.writeValueAsString(task))
	            .contentType(MediaType.APPLICATION_JSON));

        response.andExpect(MockMvcResultMatchers.status().isNotModified());
    }

    @Test
    public void  taskDeleteTest() throws Exception {
        Task task = new Task("task", true, packId1);
        int taskId = taskRepository.save(task).getId();

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .delete(String.format("/api/tasks/%s", taskId)));

        response.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void  packNotDeleteTest() throws Exception {
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .delete("/api/tasks/2"));

        response.andExpect(MockMvcResultMatchers.status().isNotModified());
    }
}