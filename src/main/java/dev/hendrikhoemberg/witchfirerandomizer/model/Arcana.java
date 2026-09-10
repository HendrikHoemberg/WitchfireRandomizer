package dev.hendrikhoemberg.witchfirerandomizer.model;

import java.util.Collections;
import java.util.List;

public class Arcana {

    private String id;
    private String name;
    private String description;
    private String effects;
    private List<String> prophecyTypes = Collections.emptyList();
    private Element element;
    private String iconUrl;

    public Arcana() {
    }

    public Arcana(String id, String name, String description, String effects, List<String> prophecyTypes, Element element, String iconUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effects = effects;
        this.prophecyTypes = prophecyTypes != null ? prophecyTypes : Collections.emptyList();
        this.element = element;
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

    public String getEffects() {
        return effects;
    }

    public void setEffects(String effects) {
        this.effects = effects;
    }

    public List<String> getProphecyTypes() {
        return prophecyTypes;
    }

    public void setProphecyTypes(List<String> prophecyTypes) {
        this.prophecyTypes = prophecyTypes != null ? prophecyTypes : Collections.emptyList();
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
