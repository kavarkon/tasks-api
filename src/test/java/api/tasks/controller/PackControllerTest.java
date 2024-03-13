package api.tasks.controller;

import api.tasks.TestContainersBaseTest;
import api.tasks.model.Pack;
import api.tasks.repository.PackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

@AutoConfigureMockMvc
class PackControllerTest extends TestContainersBaseTest {

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void packIndexTest() throws Exception {
        List<Pack> packs =
                List.of(new Pack("pack1"), new Pack("pack2"), new Pack("pack3"), new Pack("pack4"));
        packRepository.saveAll(packs);

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/packs"));

        response.andExpect(MockMvcResultMatchers.status().isOk());
        response.andExpect(MockMvcResultMatchers.jsonPath("$.size()", is(packs.size())));
    }
    @Test
    public void packCreateTest() throws Exception {
        Pack pack = new Pack("pack5");

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .post("/api/packs")
                .content(objectMapper.writeValueAsString(pack))
	            .contentType(MediaType.APPLICATION_JSON)
	            .accept(MediaType.APPLICATION_JSON));

        response.andExpect(MockMvcResultMatchers.status().isCreated());
        response.andExpect(MockMvcResultMatchers.jsonPath("$.title", is(pack.getTitle())));
    }

    @Test
    public void  packUpdateTest() throws Exception {
        int packId = packRepository.save(new Pack("pack")).getId();

        Pack pack = new Pack("updatedPack");
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .put(String.format("/api/packs/%s", packId))
                .content(objectMapper.writeValueAsString(pack))
	            .contentType(MediaType.APPLICATION_JSON));

        response.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void  packNotUpdateTest() throws Exception {
        Pack pack = new Pack("updatedPack");

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .put("/api/packs/2")
                .content(objectMapper.writeValueAsString(pack))
	            .contentType(MediaType.APPLICATION_JSON));

        response.andExpect(MockMvcResultMatchers.status().isNotModified());
    }

    @Test
    public void  packDeleteTest() throws Exception {
        Pack pack = new Pack("pack");
        int packId = packRepository.save(pack).getId();

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .delete(String.format("/api/packs/%s", packId)));

        response.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void  packNotDeleteTest() throws Exception {
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders
                .delete("/api/packs/2"));

        response.andExpect(MockMvcResultMatchers.status().isNotModified());
    }
}