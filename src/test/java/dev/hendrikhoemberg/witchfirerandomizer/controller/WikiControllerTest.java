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
class WikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testWikiIndexRendersSuccessfully() throws Exception {
        mockMvc.perform(get("/wiki"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/index"))
                .andExpect(content().string(containsString("Item Wiki")))
                .andExpect(content().string(containsString("Weapons")))
                .andExpect(content().string(containsString("Melee Weapons")));
    }

    @Test
    void testWikiItemsFragmentReturnsFilteredCards() throws Exception {
        mockMvc.perform(get("/wiki/items").param("category", "MELEE_WEAPON"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/item-grid :: itemGrid"))
                .andExpect(content().string(containsString("Morning Star")))
                .andExpect(content().string(containsString("Buckler")));
    }

    @Test
    void testWikiItemModalReturnsDetails() throws Exception {
        mockMvc.perform(get("/wiki/item/w-cricket"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/item-modal :: itemModalContent"))
                .andExpect(content().string(containsString("Cricket")))
                .andExpect(content().string(containsString("Mysterium")));
    }
}
