package api.tasks.controller;

import api.tasks.TestContainersBaseTest;
import api.tasks.model.Pack;
import api.tasks.repository.PackRepository;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

class PackControllerTest extends TestContainersBaseTest {

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void packIndexTest() throws Exception {
        List<Pack> packs =
                List.of(new Pack("pack1"), new Pack("pack2"), new Pack("pack3"), new Pack("pack4"));
        packRepository.saveAll(packs);

        ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/packs"));

        response.andExpect(MockMvcResultMatchers.status().isOk());
        response.andExpect(MockMvcResultMatchers.jsonPath("$.size()", CoreMatchers.is(packs.size())));
    }

}