package dev.hendrikhoemberg.witchfirerandomizer.model;

public enum ItemCategory {
    WEAPON("Weapons"),
    DEMONIC_WEAPON("Demonic Weapons"),
    MELEE_WEAPON("Melee Weapons"),
    LIGHT_SPELL("Light Spells"),
    HEAVY_SPELL("Heavy Spells"),
    RELIC("Relics"),
    FETISH("Fetishes"),
    RING("Rings"),
    BEAD("Beads");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
