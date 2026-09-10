package dev.hendrikhoemberg.witchfirerandomizer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The ten combat affinities an enemy can resist or be vulnerable to.
 *
 * <p>Declaration order is the canonical display order. The colour is the muted identity hue used
 * for the ledger's element dot — it is deliberately desaturated so it never competes with the two
 * semantic colours (see {@code --wf-resist} / {@code --wf-vulnerable} in {@code main.css}).
 */
public enum AffinityType {

    FIRE("Fire", "#e0705c"),
    SHOCK("Shock", "#a98bd8"),
    FREEZE("Freeze", "#7fc4e8"),
    DECAY("Decay", "#84b06a"),
    AIR("Air", "#ddc272"),
    EARTH("Earth", "#93b47a"),
    WATER("Water", "#6f9fd8"),
    BURN("Burn", "#e0904f"),
    STAGGER("Stagger", "#9aa8b8"),
    STUN("Stun", "#c8b06a");

    private final String displayName;
    private final String color;

    AffinityType(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    /** The enemy's raw value for this affinity, or {@code null} when the enemy has none. */
    public Integer valueFor(Enemy enemy) {
        if (enemy == null) {
            return null;
        }
        return switch (this) {
            case FIRE -> enemy.getFireResistance();
            case SHOCK -> enemy.getShockResistance();
            case FREEZE -> enemy.getFreezeResistance();
            case DECAY -> enemy.getDecayResistance();
            case AIR -> enemy.getAirResistance();
            case EARTH -> enemy.getEarthResistance();
            case WATER -> enemy.getWaterResistance();
            case BURN -> enemy.getBurnResistance();
            case STAGGER -> enemy.getStaggerResistance();
            case STUN -> enemy.getStunResistance();
        };
    }

    /** Affinities the enemy takes reduced damage from, in canonical order. */
    public static List<Affinity> resistances(Enemy enemy) {
        return collect(enemy, value -> value > 0);
    }

    /** Affinities the enemy takes increased damage from, in canonical order. */
    public static List<Affinity> vulnerabilities(Enemy enemy) {
        return collect(enemy, value -> value < 0);
    }

    private static List<Affinity> collect(Enemy enemy, IntPredicate keep) {
        List<Affinity> found = new ArrayList<>();
        for (AffinityType type : values()) {
            Integer value = type.valueFor(enemy);
            if (value != null && keep.test(value)) {
                found.add(new Affinity(type.displayName, type.color, value));
            }
        }
        return List.copyOf(found);
    }
}
