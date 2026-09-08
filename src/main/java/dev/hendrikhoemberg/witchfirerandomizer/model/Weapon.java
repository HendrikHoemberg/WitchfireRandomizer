package dev.hendrikhoemberg.witchfirerandomizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Weapon extends Item {
    private String rangeCategory;
    private String weaponFamily;
    private int damage;
    private int criticalDamage;
    private String stunPower;
    private double adsRange;
    private double hipfireRange;
    private double rateOfFire;
    private double reloadSpeed;
    private String stability;
    private String mobility;
    private int magSize;
    private int ammoReserves;
    private String fireMode;

    public String getRangeCategory() { return rangeCategory; }
    public void setRangeCategory(String rangeCategory) { this.rangeCategory = rangeCategory; }

    public String getWeaponFamily() { return weaponFamily; }
    public void setWeaponFamily(String weaponFamily) { this.weaponFamily = weaponFamily; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getCriticalDamage() { return criticalDamage; }
    public void setCriticalDamage(int criticalDamage) { this.criticalDamage = criticalDamage; }

    public String getStunPower() { return stunPower; }
    public void setStunPower(String stunPower) { this.stunPower = stunPower; }

    public double getAdsRange() { return adsRange; }
    public void setAdsRange(double adsRange) { this.adsRange = adsRange; }

    public double getHipfireRange() { return hipfireRange; }
    public void setHipfireRange(double hipfireRange) { this.hipfireRange = hipfireRange; }

    public double getRateOfFire() { return rateOfFire; }
    public void setRateOfFire(double rateOfFire) { this.rateOfFire = rateOfFire; }

    public double getReloadSpeed() { return reloadSpeed; }
    public void setReloadSpeed(double reloadSpeed) { this.reloadSpeed = reloadSpeed; }

    public String getStability() { return stability; }
    public void setStability(String stability) { this.stability = stability; }

    public String getMobility() { return mobility; }
    public void setMobility(String mobility) { this.mobility = mobility; }

    public int getMagSize() { return magSize; }
    public void setMagSize(int magSize) { this.magSize = magSize; }

    public int getAmmoReserves() { return ammoReserves; }
    public void setAmmoReserves(int ammoReserves) { this.ammoReserves = ammoReserves; }

    public String getFireMode() { return fireMode; }
    public void setFireMode(String fireMode) { this.fireMode = fireMode; }
}
