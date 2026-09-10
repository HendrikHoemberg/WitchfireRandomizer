package dev.hendrikhoemberg.witchfirerandomizer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WikiNavigationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderSubNavWithEquipmentArcanaPropheciesAndBestiaryTabs() throws Exception {
        mockMvc.perform(get("/wiki"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Equipment")))
                .andExpect(content().string(containsString("/wiki/arcana")))
                .andExpect(content().string(containsString("/wiki/prophecies")))
                .andExpect(content().string(containsString("/wiki/bestiary")));

        mockMvc.perform(get("/wiki/prophecies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/wiki/prophecies\" class=\"wiki-tab-link active\"")));
    }
}
