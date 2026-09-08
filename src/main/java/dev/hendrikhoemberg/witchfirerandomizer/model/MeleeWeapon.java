package dev.hendrikhoemberg.witchfirerandomizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeleeWeapon extends Item {
    private int baseDamage;
    private int chargedDamage;
    private String specialAttack;
    private String specialDamage;

    public int getBaseDamage() { return baseDamage; }
    public void setBaseDamage(int baseDamage) { this.baseDamage = baseDamage; }

    public int getChargedDamage() { return chargedDamage; }
    public void setChargedDamage(int chargedDamage) { this.chargedDamage = chargedDamage; }

    public String getSpecialAttack() { return specialAttack; }
    public void setSpecialAttack(String specialAttack) { this.specialAttack = specialAttack; }

    public String getSpecialDamage() { return specialDamage; }
    public void setSpecialDamage(String specialDamage) { this.specialDamage = specialDamage; }
}
