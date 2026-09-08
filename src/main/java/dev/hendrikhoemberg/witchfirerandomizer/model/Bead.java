package dev.hendrikhoemberg.witchfirerandomizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bead extends Item {
    private List<BeadRequirement> requirements = new ArrayList<>();

    public List<BeadRequirement> getRequirements() { return requirements; }
    public void setRequirements(List<BeadRequirement> requirements) {
        this.requirements = requirements != null ? requirements : new ArrayList<>();
    }
}
