package dev.hendrikhoemberg.witchfirerandomizer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AffinityTest {

    private static Enemy anointer() {
        Enemy e = new Enemy();
        e.setName("Anointer");
        e.setFireResistance(75);
        e.setFreezeResistance(50);
        e.setDecayResistance(-100);
        e.setAirResistance(35);
        e.setStaggerResistance(80);
        return e;
    }

    @Test
    void shouldSplitAffinitiesIntoResistancesAndVulnerabilities() {
        Enemy e = anointer();

        assertEquals(List.of("Fire", "Freeze", "Air", "Stagger"),
                e.getResistances().stream().map(Affinity::name).toList());
        assertEquals(List.of("Decay"),
                e.getVulnerabilities().stream().map(Affinity::name).toList());
    }

    @Test
    void shouldIgnoreZeroAndMissingResistances() {
        Enemy e = new Enemy();
        e.setFireResistance(0);
        e.setWaterResistance(null);

        assertTrue(e.getResistances().isEmpty());
        assertTrue(e.getVulnerabilities().isEmpty());
    }

    @Test
    void shouldClampBarStrengthButKeepTheExactValue() {
        Enemy e = new Enemy();
        e.setFireResistance(-200);

        Affinity decay = e.getVulnerabilities().getFirst();
        assertTrue(decay.vulnerable());
        assertEquals(-200, decay.value());
        assertEquals(100, decay.strength());
        assertEquals("-200%", decay.displayValue());
    }

    @Test
    void shouldScaleBarIntensityWithMagnitude() {
        Enemy e = new Enemy();
        e.setFireResistance(100);
        e.setAirResistance(5);

        assertEquals(100, e.getResistances().get(0).intensityPercent());
        assertEquals(57, e.getResistances().get(1).intensityPercent());
    }

    @Test
    void shouldFormatPositiveValuesWithAPlusSign() {
        Enemy e = anointer();

        assertEquals("+75%", e.getResistances().getFirst().displayValue());
        assertEquals(75, e.getResistances().getFirst().strength());
        assertFalse(e.getResistances().getFirst().vulnerable());
    }

    @Test
    void shouldDescribeEveryAffinityTypeWithANameAndAColour() {
        assertEquals(10, AffinityType.values().length);

        for (AffinityType type : AffinityType.values()) {
            assertNotNull(type.getDisplayName(), "display name for " + type);
            assertTrue(type.getColor().matches("#[0-9a-f]{6}"),
                    type + " should carry a hex colour but was " + type.getColor());
        }
    }

    @Test
    void shouldReadEachAffinityTypeFromTheEnemy() {
        Enemy e = anointer();

        assertEquals(75, AffinityType.FIRE.valueFor(e));
        assertEquals(80, AffinityType.STAGGER.valueFor(e));
        assertEquals(-100, AffinityType.DECAY.valueFor(e));
        assertNull(AffinityType.SHOCK.valueFor(e));
    }
}
