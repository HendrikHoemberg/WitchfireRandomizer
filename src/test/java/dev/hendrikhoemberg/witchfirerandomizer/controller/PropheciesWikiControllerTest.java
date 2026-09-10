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
class PropheciesWikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderPropheciesPage() throws Exception {
        mockMvc.perform(get("/wiki/prophecies"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/prophecies"))
                .andExpect(model().attributeExists("prophecies", "locations"))
                .andExpect(content().string(containsString("Prophecies &amp; Omens")))
                .andExpect(content().string(containsString("Prophecy of Fire")))
                .andExpect(content().string(containsString("Omen")));
    }

    @Test
    void shouldFilterPropheciesListFragment() throws Exception {
        mockMvc.perform(get("/wiki/prophecies/list").param("search", "Fire"))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki/fragments/prophecy-list :: prophecyList"))
                .andExpect(content().string(containsString("Prophecy of Fire")));
    }
}
