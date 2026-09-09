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
class LegalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPrivacyPolicyPageRenders() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/privacy"))
                .andExpect(content().string(containsString("Privacy Policy")))
                .andExpect(content().string(containsString("No Cookies &amp; No Tracking")))
                .andExpect(content().string(containsString("Local Storage")));
    }

    @Test
    void testImpressumPageRenders() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/impressum"))
                .andExpect(content().string(containsString("Impressum")))
                .andExpect(content().string(containsString("Legal Notice")))
                .andExpect(content().string(containsString("The Astronauts")));
    }

    @Test
    void testLegalPagesIncludeFooter() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("site-footer")))
                .andExpect(content().string(containsString("Witchfire Randomizer 1.0.0")))
                .andExpect(content().string(containsString("Gregory Pedzinski")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("href=\"/impressum\"")));
    }
}
