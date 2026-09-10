package dev.hendrikhoemberg.witchfirerandomizer.model;

public class Prophecy {

    private String id;
    private String name;
    private String description;
    private String arcanaType;
    private String omenName;
    private String omenEffect;
    private String location;
    private String iconUrl;

    public Prophecy() {
    }

    public Prophecy(String id, String name, String description, String arcanaType, String omenName, String omenEffect, String location, String iconUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.arcanaType = arcanaType;
        this.omenName = omenName;
        this.omenEffect = omenEffect;
        this.location = location;
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

    public String getArcanaType() {
        return arcanaType;
    }

    public void setArcanaType(String arcanaType) {
        this.arcanaType = arcanaType;
    }

    public String getOmenName() {
        return omenName;
    }

    public void setOmenName(String omenName) {
        this.omenName = omenName;
    }

    public String getOmenEffect() {
        return omenEffect;
    }

    public void setOmenEffect(String omenEffect) {
        this.omenEffect = omenEffect;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
