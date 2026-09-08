package dev.hendrikhoemberg.witchfirerandomizer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void testWeaponCreationAndAttributes() {
        Weapon weapon = new Weapon();
        weapon.setId("w-cricket");
        weapon.setName("Cricket");
        weapon.setCategory(ItemCategory.WEAPON);
        weapon.setDamage(17);
        weapon.setWeaponFamily("Machine Pistol");
        weapon.setRangeCategory("Close Range Weapon");

        assertEquals("w-cricket", weapon.getId());
        assertEquals("Cricket", weapon.getName());
        assertEquals(ItemCategory.WEAPON, weapon.getCategory());
        assertEquals(17, weapon.getDamage());
        assertEquals("Machine Pistol", weapon.getWeaponFamily());
    }

    @Test
    void testMeleeWeaponCreationAndAttributes() {
        MeleeWeapon melee = new MeleeWeapon();
        melee.setId("mw-morning-star");
        melee.setName("Morning Star");
        melee.setCategory(ItemCategory.MELEE_WEAPON);
        melee.setBaseDamage(20);
        melee.setChargedDamage(200);
        melee.setSpecialAttack("Crouch slam");

        assertEquals("mw-morning-star", melee.getId());
        assertEquals(ItemCategory.MELEE_WEAPON, melee.getCategory());
        assertEquals(20, melee.getBaseDamage());
        assertEquals(200, melee.getChargedDamage());
        assertEquals("Crouch slam", melee.getSpecialAttack());
    }

    @Test
    void testBeadCreationWithRequirements() {
        Bead bead = new Bead();
        bead.setId("b-acute");
        bead.setName("Acute Ailment Bead");
        bead.setCategory(ItemCategory.BEAD);
        bead.setRequirements(List.of(new BeadRequirement("Witchery", 30)));

        assertEquals(1, bead.getRequirements().size());
        assertEquals("Witchery", bead.getRequirements().getFirst().stat());
        assertEquals(30, bead.getRequirements().getFirst().value());
    }

    @Test
    void testLoadoutContainsMeleeWeaponSlot() {
        Loadout loadout = new Loadout();
        MeleeWeapon melee = new MeleeWeapon();
        melee.setName("Katar");
        loadout.setMeleeWeapon(melee);

        assertNotNull(loadout.getMeleeWeapon());
        assertEquals("Katar", loadout.getMeleeWeapon().getName());
    }
}
