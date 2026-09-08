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
    void testRerollLoadoutReturnsUpdatedGrid() throws Exception {
        mockMvc.perform(post("/randomizer/reroll")
                        .param("locks[meleeWeapon]", "true")
                        .param("currentSlotItemIds[meleeWeapon]", "mw-morning-star"))
                .andExpect(status().isOk())
                .andExpect(view().name("randomizer/fragments/loadout-grid :: loadoutGrid"))
                .andExpect(content().string(containsString("Morning Star")));
    }
}
