package dev.hendrikhoemberg.witchfirerandomizer.model;

import java.util.ArrayList;
import java.util.List;

public class Loadout {
    private Weapon primaryWeapon;
    private Weapon secondaryWeapon;
    private Weapon demonicWeapon;
    private MeleeWeapon meleeWeapon;
    private Spell lightSpell;
    private Spell heavySpell;
    private MagicalItem relic;
    private MagicalItem fetish;
    private MagicalItem ring;
    private List<Bead> beads = new ArrayList<>();

    public Weapon getPrimaryWeapon() { return primaryWeapon; }
    public void setPrimaryWeapon(Weapon primaryWeapon) { this.primaryWeapon = primaryWeapon; }

    public Weapon getSecondaryWeapon() { return secondaryWeapon; }
    public void setSecondaryWeapon(Weapon secondaryWeapon) { this.secondaryWeapon = secondaryWeapon; }

    public Weapon getDemonicWeapon() { return demonicWeapon; }
    public void setDemonicWeapon(Weapon demonicWeapon) { this.demonicWeapon = demonicWeapon; }

    public MeleeWeapon getMeleeWeapon() { return meleeWeapon; }
    public void setMeleeWeapon(MeleeWeapon meleeWeapon) { this.meleeWeapon = meleeWeapon; }

    public Spell getLightSpell() { return lightSpell; }
    public void setLightSpell(Spell lightSpell) { this.lightSpell = lightSpell; }

    public Spell getHeavySpell() { return heavySpell; }
    public void setHeavySpell(Spell heavySpell) { this.heavySpell = heavySpell; }

    public MagicalItem getRelic() { return relic; }
    public void setRelic(MagicalItem relic) { this.relic = relic; }

    public MagicalItem getFetish() { return fetish; }
    public void setFetish(MagicalItem fetish) { this.fetish = fetish; }

    public MagicalItem getRing() { return ring; }
    public void setRing(MagicalItem ring) { this.ring = ring; }

    public List<Bead> getBeads() { return beads; }
    public void setBeads(List<Bead> beads) { this.beads = beads != null ? beads : new ArrayList<>(); }
}
