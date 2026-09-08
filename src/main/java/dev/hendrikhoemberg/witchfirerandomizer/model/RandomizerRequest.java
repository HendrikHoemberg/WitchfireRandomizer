package dev.hendrikhoemberg.witchfirerandomizer.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RandomizerRequest {
    private Map<String, Boolean> locks = new HashMap<>();
    private Map<String, String> currentSlotItemIds = new HashMap<>();
    private Set<Element> preferredElements = new HashSet<>();
    private Set<String> excludedItemIds = new HashSet<>();
    private Map<String, Integer> beadUserStats = new HashMap<>();
    private int beadSlotCount = 5;

    public Map<String, Boolean> getLocks() { return locks; }
    public void setLocks(Map<String, Boolean> locks) { this.locks = locks != null ? locks : new HashMap<>(); }

    public Map<String, String> getCurrentSlotItemIds() { return currentSlotItemIds; }
    public void setCurrentSlotItemIds(Map<String, String> currentSlotItemIds) { this.currentSlotItemIds = currentSlotItemIds != null ? currentSlotItemIds : new HashMap<>(); }

    public Set<Element> getPreferredElements() { return preferredElements; }
    public void setPreferredElements(Set<Element> preferredElements) { this.preferredElements = preferredElements != null ? preferredElements : new HashSet<>(); }

    public Set<String> getExcludedItemIds() { return excludedItemIds; }
    public void setExcludedItemIds(Set<String> excludedItemIds) { this.excludedItemIds = excludedItemIds != null ? excludedItemIds : new HashSet<>(); }

    public Map<String, Integer> getBeadUserStats() { return beadUserStats; }
    public void setBeadUserStats(Map<String, Integer> beadUserStats) { this.beadUserStats = beadUserStats != null ? beadUserStats : new HashMap<>(); }

    public int getBeadSlotCount() { return beadSlotCount; }
    public void setBeadSlotCount(int beadSlotCount) { this.beadSlotCount = beadSlotCount; }
}
