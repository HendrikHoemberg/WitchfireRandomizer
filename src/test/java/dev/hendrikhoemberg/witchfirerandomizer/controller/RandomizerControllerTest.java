package dev.hendrikhoemberg.witchfirerandomizer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RandomizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRandomizerIndexRendersAllSlotsIncludingMelee() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("randomizer/index"))
                .andExpect(content().string(containsString("Randomizer")))
                .andExpect(content().string(containsString("Primary Weapon")))
                .andExpect(content().string(containsString("Secondary Weapon")))
                .andExpect(content().string(containsString("Demonic Weapon")))
                .andExpect(content().string(containsString("Melee Weapon")))
                .andExpect(content().string(containsString("Light Spell")))
                .andExpect(content().string(containsString("Heavy Spell")))
                .andExpect(content().string(containsString("Relic")))
                .andExpect(content().string(containsString("Fetish")))
                .andExpect(content().string(containsString("Ring")))
                .andExpect(content().string(containsString("Beads")));
    }

    @Test
    void testHeaderAndDrawerNavigationRendered() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wf-logo2.webP")))
                .andExpect(content().string(containsString("burger-btn")))
                .andExpect(content().string(containsString("Loadout Randomizer")))
                .andExpect(content().string(containsString("Item Wiki")));
    }

    @Test
    void testCurrentLoadoutCardStructure() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Current Loadout")))
                .andExpect(content().string(containsString("Generate Share Link")))
                .andExpect(content().string(containsString("Clear All")))
                .andExpect(content().string(containsString("Click on a Slot to lock/unlock it.")))
                .andExpect(content().string(containsString("Generate New Loadout")));
    }

    @Test
    void testBeadsCardAndStatRequirementsStructure() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Beads")))
                .andExpect(content().string(containsString("Stat Requirements for Current Beads")))
                .andExpect(content().string(containsString("Flesh")))
                .andExpect(content().string(containsString("Blood")))
                .andExpect(content().string(containsString("Mind")))
                .andExpect(content().string(containsString("Witchery")))
                .andExpect(content().string(containsString("Arsenal")))
                .andExpect(content().string(containsString("Faith")));
    }

    @Test
    void testElementPreferencesAndEmptySlotMode() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Element Preferences")))
                .andExpect(content().string(containsString("Clear All Preferences")))
                .andExpect(content().string(containsString("Empty Slot Mode")));
    }

    @Test
    void testItemPopupTemplatesRendered() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"item-card-popup-inner\"")))
                .andExpect(content().string(containsString("popup-template")))
                .andExpect(content().string(containsString("id=\"hover-item-popup\"")));
    }

    @Test
    void testRerollLoadoutIncludesPopupTemplates() throws Exception {
        mockMvc.perform(post("/randomizer/reroll"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("popup-template")))
                .andExpect(content().string(containsString("item-card-popup-inner")));
    }

    @Test
    void testPopupIncludesElementDot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<span class=\"element-dot\"")));
    }

    @Test
    void testSlotCardIncludesWhiteFillOverlayAndGlow() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("slot-white-overlay")))
                .andExpect(content().string(containsString("animate-glow-pulse shadow-glow")));
    }

    @Test
    void testRerollFragmentIncludesWhiteFillOverlayAndGlow() throws Exception {
        mockMvc.perform(post("/randomizer/reroll"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("slot-white-overlay")))
                .andExpect(content().string(containsString("animate-glow-pulse shadow-glow")));
    }
}

