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
    void testPreservesLockedEmptySlots() {
        RandomizerRequest request = new RandomizerRequest();
        request.setLocks(Map.of("primaryWeapon", true, "meleeWeapon", true, "relic", true));
        request.setCurrentSlotItemIds(Map.of("primaryWeapon", "", "meleeWeapon", ""));

        Loadout loadout = randomizerService.generateRandomLoadout(request);

        assertNull(loadout.getPrimaryWeapon(), "Locked empty primary weapon must remain null");
        assertNull(loadout.getMeleeWeapon(), "Locked empty melee weapon must remain null");
        assertNull(loadout.getRelic(), "Locked slot with no current id must remain null");
        assertNotNull(loadout.getDemonicWeapon(), "Unlocked demonic weapon should still be generated");
    }

    @Test
    void testPreservesLockedBeads() {
        RandomizerRequest request = new RandomizerRequest();
        request.setBeadSlotCount(3);
        Bead testBead = itemRepository.findEligibleBeads(Map.of()).get(0);
        request.setLocks(Map.of("bead1", true, "bead2", true));
        request.setCurrentSlotItemIds(Map.of("bead1", testBead.getId(), "bead2", ""));

        Loadout loadout = randomizerService.generateRandomLoadout(request);

        assertEquals(3, loadout.getBeads().size());
        assertNotNull(loadout.getBeads().get(0));
        assertEquals(testBead.getId(), loadout.getBeads().get(0).getId(), "Locked bead must remain unchanged");
        assertNull(loadout.getBeads().get(1), "Locked empty bead slot must remain null");
        assertNotNull(loadout.getBeads().get(2), "Unlocked bead slot should be generated");
        assertNotEquals(testBead.getId(), loadout.getBeads().get(2).getId(), "Unlocked bead slot should not duplicate locked bead");
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
