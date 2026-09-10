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
class ArcanaWikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderArcanaPage() throws Exception {
        mockMvc.perform(get("/wiki/arcana"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/arcana"))
                .andExpect(model().attributeExists("cards", "prophecyTypes", "elements"))
                .andExpect(content().string(containsString("Arcana")))
                .andExpect(content().string(containsString("Accelerant")));
    }

    @Test
    void shouldReturnCardGridFragment() throws Exception {
        mockMvc.perform(get("/wiki/arcana/cards").param("element", "Fire"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/arcana-grid :: arcanaGrid"))
                .andExpect(content().string(containsString("Accelerant")));
    }

    @Test
    void shouldReturnCardModalFragment() throws Exception {
        mockMvc.perform(get("/wiki/arcana/card/arcana-accelerant"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/arcana-modal :: arcanaModalContent"))
                .andExpect(content().string(containsString("Accelerant")))
                .andExpect(content().string(containsString("Burning enemies receive even more damage.")));
    }

    @Test
    void shouldReturn404ForMissingCard() throws Exception {
        mockMvc.perform(get("/wiki/arcana/card/missing-card-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnProphecyListFragment() throws Exception {
        mockMvc.perform(get("/wiki/arcana/prophecies"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/prophecy-list :: prophecyList"))
                .andExpect(content().string(containsString("Prophecy of Fire")))
                .andExpect(content().string(containsString("Omen")));
    }
}
