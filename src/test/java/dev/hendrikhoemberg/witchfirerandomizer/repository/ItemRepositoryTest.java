package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ItemRepositoryTest {

    private ItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ItemRepository();
        repository.init();
    }

    @Test
    void testLoadsAllItemsFromItemsJson() {
        List<Item> items = repository.findAll();
        assertNotNull(items);
        assertTrue(items.size() >= 100, "Should load at least 100 items from items.json");
    }

    @Test
    void testFindMeleeWeapons() {
        List<Item> meleeWeapons = repository.findByCategory(ItemCategory.MELEE_WEAPON);
        assertFalse(meleeWeapons.isEmpty(), "Should find melee weapons");
        assertTrue(meleeWeapons.stream().anyMatch(i -> i.getName().equals("Morning Star")));
        assertTrue(meleeWeapons.getFirst() instanceof MeleeWeapon);
    }

    @Test
    void testFindById() {
        Optional<Item> item = repository.findById("w-cricket");
        assertTrue(item.isPresent());
        assertEquals("Cricket", item.get().getName());
        assertTrue(item.get() instanceof Weapon);
    }

    @Test
    void testSearchByQueryAndCategory() {
        List<Item> results = repository.search(ItemCategory.WEAPON, null, "hunger");
        assertEquals(1, results.size());
        assertEquals("Hunger", results.getFirst().getName());
    }

    @Test
    void testSearchByElement() {
        List<Item> waterSpells = repository.search(ItemCategory.LIGHT_SPELL, Element.Water, null);
        assertFalse(waterSpells.isEmpty());
        assertTrue(waterSpells.stream().allMatch(i -> i.getElement() == Element.Water));
    }

    @Test
    void testFindEligibleBeadsRespectsStats() {
        // High stats should find all beads with requirements <= 50
        Map<String, Integer> highStats = Map.of(
            "Flesh", 100,
            "Blood", 100,
            "Mind", 100,
            "Witchery", 100,
            "Arsenal", 100,
            "Faith", 100
        );
        List<Bead> allBeads = repository.findEligibleBeads(highStats);
        assertFalse(allBeads.isEmpty());

        // Zero stats should only find beads that have no requirements
        Map<String, Integer> zeroStats = Map.of(
            "Flesh", 0,
            "Blood", 0,
            "Mind", 0,
            "Witchery", 0,
            "Arsenal", 0,
            "Faith", 0
        );
        List<Bead> noReqBeads = repository.findEligibleBeads(zeroStats);
        assertTrue(noReqBeads.stream().allMatch(b -> b.getRequirements().isEmpty()));
    }
}
