package dev.hendrikhoemberg.witchfirerandomizer.service;

import dev.hendrikhoemberg.witchfirerandomizer.model.*;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerServiceTest {

    private ItemRepository itemRepository;
    private RandomizerService randomizerService;

    @BeforeEach
    void setUp() {
        itemRepository = new ItemRepository();
        itemRepository.init();
        randomizerService = new RandomizerService(itemRepository);
    }

    @Test
    void testGeneratesCompleteLoadout() {
        RandomizerRequest request = new RandomizerRequest();
        Loadout loadout = randomizerService.generateRandomLoadout(request);

        assertNotNull(loadout);
        assertNotNull(loadout.getPrimaryWeapon());
        assertNotNull(loadout.getSecondaryWeapon());
        assertNotNull(loadout.getDemonicWeapon());
        assertNotNull(loadout.getMeleeWeapon(), "Melee weapon slot must not be null");
        assertNotNull(loadout.getLightSpell());
        assertNotNull(loadout.getHeavySpell());
        assertNotNull(loadout.getRelic());
        assertNotNull(loadout.getFetish());
        assertNotNull(loadout.getRing());
        assertFalse(loadout.getBeads().isEmpty(), "Should equip beads");

        // Primary and secondary must be distinct standard weapons
        assertNotEquals(loadout.getPrimaryWeapon().getId(), loadout.getSecondaryWeapon().getId());
        assertEquals(ItemCategory.WEAPON, loadout.getPrimaryWeapon().getCategory());
        assertEquals(ItemCategory.WEAPON, loadout.getSecondaryWeapon().getCategory());
        assertEquals(ItemCategory.DEMONIC_WEAPON, loadout.getDemonicWeapon().getCategory());
        assertEquals(ItemCategory.MELEE_WEAPON, loadout.getMeleeWeapon().getCategory());
    }

    @Test
    void testPreservesLockedSlots() {
        RandomizerRequest request = new RandomizerRequest();
        request.setLocks(Map.of("meleeWeapon", true, "primaryWeapon", true));
        request.setCurrentSlotItemIds(Map.of("meleeWeapon", "mw-morning-star", "primaryWeapon", "w-cricket"));

        Loadout loadout = randomizerService.generateRandomLoadout(request);

        assertEquals("mw-morning-star", loadout.getMeleeWeapon().getId());
        assertEquals("w-cricket", loadout.getPrimaryWeapon().getId());
    }

    @Test
    void testExcludesItems() {
        RandomizerRequest request = new RandomizerRequest();
        // Exclude all melee weapons except one, or exclude a specific one
        request.setExcludedItemIds(Set.of("mw-fist", "mw-buckler", "mw-katar", "mw-sacring-bell", "mw-zweihander"));

        Loadout loadout = randomizerService.generateRandomLoadout(request);
        assertEquals("mw-morning-star", loadout.getMeleeWeapon().getId(), "Should pick the only non-excluded melee weapon");
    }

    @Test
    void testRerollSingleSlot() {
        RandomizerRequest request = new RandomizerRequest();
        Item item = randomizerService.rerollSlot("meleeWeapon", request);

        assertNotNull(item);
        assertTrue(item instanceof MeleeWeapon);
        assertEquals(ItemCategory.MELEE_WEAPON, item.getCategory());
    }
}
