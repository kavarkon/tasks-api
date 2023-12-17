package api.tasks.controller;

import api.tasks.AbstractContainerBaseTest;
import api.tasks.model.Pack;
import api.tasks.repository.PackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackControllerTest extends AbstractContainerBaseTest throws Exception {

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void givenPacks_whenGetAllPacks_thenListOfPacks() throws Exception {
        List<Pack> packs =
                List.of(new Pack("pack1"), new Pack("pack2"), new Pack("pack3"), new Pack("pack4"));
        packRepository.
    }

}