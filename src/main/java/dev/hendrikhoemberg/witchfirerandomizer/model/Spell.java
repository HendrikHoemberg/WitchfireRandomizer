package dev.hendrikhoemberg.witchfirerandomizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Spell extends Item {
    private int charges;
    private String recharge;

    public int getCharges() { return charges; }
    public void setCharges(int charges) { this.charges = charges; }

    public String getRecharge() { return recharge; }
    public void setRecharge(String recharge) { this.recharge = recharge; }
}
