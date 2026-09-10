package dev.hendrikhoemberg.witchfirerandomizer.model;

/**
 * A single active affinity on an enemy, ready to render in the bestiary ledger.
 *
 * @param name  display name of the element or effect, e.g. {@code Fire}
 * @param color muted identity hue for the entry's dot
 * @param value raw resistance percentage; negative means the enemy is vulnerable
 */
public record Affinity(String name, String color, int value) {

    public boolean vulnerable() {
        return value < 0;
    }

    /** Bar length as a percentage, clamped at 100 so the rare -200% does not overflow the track. */
    public int strength() {
        return Math.min(Math.abs(value), 100);
    }

    /** Bar opacity as a percentage: a +100% resistance reads heavier than a +5% one. */
    public int intensityPercent() {
        return 55 + Math.round(45f * strength() / 100f);
    }

    public String displayValue() {
        return (value > 0 ? "+" : "-") + Math.abs(value) + "%";
    }
}
