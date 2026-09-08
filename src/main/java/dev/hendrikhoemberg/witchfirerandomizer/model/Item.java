package dev.hendrikhoemberg.witchfirerandomizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "category", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Weapon.class, name = "WEAPON"),
    @JsonSubTypes.Type(value = Weapon.class, name = "DEMONIC_WEAPON"),
    @JsonSubTypes.Type(value = MeleeWeapon.class, name = "MELEE_WEAPON"),
    @JsonSubTypes.Type(value = Spell.class, name = "LIGHT_SPELL"),
    @JsonSubTypes.Type(value = Spell.class, name = "HEAVY_SPELL"),
    @JsonSubTypes.Type(value = MagicalItem.class, name = "RELIC"),
    @JsonSubTypes.Type(value = MagicalItem.class, name = "FETISH"),
    @JsonSubTypes.Type(value = MagicalItem.class, name = "RING"),
    @JsonSubTypes.Type(value = Bead.class, name = "BEAD")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    private String id;
    private String name;
    private ItemCategory category;
    private Element element;
    private String description;
    private String iconUrl;
    private String location;
    private List<MysteriumTier> mysteriumTiers = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public Element getElement() { return element; }
    public void setElement(Element element) { this.element = element; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<MysteriumTier> getMysteriumTiers() { return mysteriumTiers; }
    public void setMysteriumTiers(List<MysteriumTier> mysteriumTiers) { this.mysteriumTiers = mysteriumTiers != null ? mysteriumTiers : new ArrayList<>(); }
}
