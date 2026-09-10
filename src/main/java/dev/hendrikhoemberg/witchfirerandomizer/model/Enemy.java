package dev.hendrikhoemberg.witchfirerandomizer.model;

import java.util.Collections;
import java.util.List;

public class Enemy {

    private String id;
    private String name;
    private String description;
    private String rank;
    private Integer gnosis;
    private Integer health;
    private String damage;
    private String variants;
    private List<String> locations = Collections.emptyList();
    private Integer fireResistance;
    private Integer earthResistance;
    private Integer waterResistance;
    private Integer airResistance;
    private Integer burnResistance;
    private Integer decayResistance;
    private Integer freezeResistance;
    private Integer shockResistance;
    private Integer stunResistance;
    private Integer staggerResistance;
    private String iconUrl;

    public Enemy() {
    }

    public Enemy(String id, String name, String description, String rank, Integer gnosis, Integer health, String damage, String variants, List<String> locations, Integer fireResistance, Integer earthResistance, Integer waterResistance, Integer airResistance, Integer burnResistance, Integer decayResistance, Integer freezeResistance, Integer shockResistance, Integer stunResistance, Integer staggerResistance, String iconUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rank = rank;
        this.gnosis = gnosis;
        this.health = health;
        this.damage = damage;
        this.variants = variants;
        this.locations = locations != null ? locations : Collections.emptyList();
        this.fireResistance = fireResistance;
        this.earthResistance = earthResistance;
        this.waterResistance = waterResistance;
        this.airResistance = airResistance;
        this.burnResistance = burnResistance;
        this.decayResistance = decayResistance;
        this.freezeResistance = freezeResistance;
        this.shockResistance = shockResistance;
        this.stunResistance = stunResistance;
        this.staggerResistance = staggerResistance;
        this.iconUrl = iconUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Integer getGnosis() {
        return gnosis;
    }

    public void setGnosis(Integer gnosis) {
        this.gnosis = gnosis;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public String getDamage() {
        return damage;
    }

    public void setDamage(String damage) {
        this.damage = damage;
    }

    public String getVariants() {
        return variants;
    }

    public void setVariants(String variants) {
        this.variants = variants;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations != null ? locations : Collections.emptyList();
    }

    public Integer getFireResistance() {
        return fireResistance;
    }

    public void setFireResistance(Integer fireResistance) {
        this.fireResistance = fireResistance;
    }

    public Integer getEarthResistance() {
        return earthResistance;
    }

    public void setEarthResistance(Integer earthResistance) {
        this.earthResistance = earthResistance;
    }

    public Integer getWaterResistance() {
        return waterResistance;
    }

    public void setWaterResistance(Integer waterResistance) {
        this.waterResistance = waterResistance;
    }

    public Integer getAirResistance() {
        return airResistance;
    }

    public void setAirResistance(Integer airResistance) {
        this.airResistance = airResistance;
    }

    public Integer getBurnResistance() {
        return burnResistance;
    }

    public void setBurnResistance(Integer burnResistance) {
        this.burnResistance = burnResistance;
    }

    public Integer getDecayResistance() {
        return decayResistance;
    }

    public void setDecayResistance(Integer decayResistance) {
        this.decayResistance = decayResistance;
    }

    public Integer getFreezeResistance() {
        return freezeResistance;
    }

    public void setFreezeResistance(Integer freezeResistance) {
        this.freezeResistance = freezeResistance;
    }

    public Integer getShockResistance() {
        return shockResistance;
    }

    public void setShockResistance(Integer shockResistance) {
        this.shockResistance = shockResistance;
    }

    public Integer getStunResistance() {
        return stunResistance;
    }

    public void setStunResistance(Integer stunResistance) {
        this.stunResistance = stunResistance;
    }

    public Integer getStaggerResistance() {
        return staggerResistance;
    }

    public void setStaggerResistance(Integer staggerResistance) {
        this.staggerResistance = staggerResistance;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    /** Active resistances, strongest-first grouping handled by the view; canonical order here. */
    public List<Affinity> getResistances() {
        return AffinityType.resistances(this);
    }

    /** Active vulnerabilities, i.e. the elements this enemy takes extra damage from. */
    public List<Affinity> getVulnerabilities() {
        return AffinityType.vulnerabilities(this);
    }
}
