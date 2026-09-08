package dev.hendrikhoemberg.witchfirerandomizer.model;

import java.util.*;

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

    public List<Element> getActiveElements() {
        Set<Element> active = new LinkedHashSet<>();
        List<Item> items = Arrays.asList(primaryWeapon, secondaryWeapon, demonicWeapon, meleeWeapon, lightSpell, heavySpell, relic, fetish, ring);
        for (Item i : items) {
            if (i != null && i.getElement() != null) {
                active.add(i.getElement());
            }
        }
        return new ArrayList<>(active);
    }

    public Map<String, Integer> getStatRequirements() {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        reqs.put("Flesh", 0);
        reqs.put("Blood", 0);
        reqs.put("Mind", 0);
        reqs.put("Witchery", 0);
        reqs.put("Arsenal", 0);
        reqs.put("Faith", 0);
        if (beads != null) {
            for (Bead b : beads) {
                if (b != null && b.getRequirements() != null) {
                    for (BeadRequirement br : b.getRequirements()) {
                        String stat = br.stat();
                        if (reqs.containsKey(stat)) {
                            reqs.put(stat, Math.max(reqs.get(stat), br.value()));
                        }
                    }
                }
            }
        }
        return reqs;
    }
}
