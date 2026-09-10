package dev.hendrikhoemberg.witchfirerandomizer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BestiaryWikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderBestiaryPage() throws Exception {
        mockMvc.perform(get("/wiki/bestiary"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/bestiary"))
                .andExpect(model().attributeExists("enemies", "ranks", "locations"))
                .andExpect(content().string(containsString("Bestiary")))
                .andExpect(content().string(containsString("Anointer")))
                .andExpect(content().string(containsString("Arcabusier")));
    }

    @Test
    void shouldReturnEnemyGridFragment() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemies").param("rank", "Faithful"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/enemy-grid :: enemyGrid"))
                .andExpect(content().string(containsString("Anointer")));
    }

    @Test
    void shouldReturnEnemyModalFragment() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemy/enemy-anointer"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/enemy-modal :: enemyModalContent"))
                .andExpect(content().string(containsString("Anointer")))
                .andExpect(content().string(containsString("Faithful")))
                .andExpect(content().string(containsString("Irongate Castle")));
    }

    @Test
    void shouldReturn404ForMissingEnemy() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemy/missing-enemy-id"))
                .andExpect(status().isNotFound());
    }
}
