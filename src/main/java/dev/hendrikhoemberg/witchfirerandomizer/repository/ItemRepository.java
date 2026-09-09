package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ItemRepository {

    private static final Logger logger = LoggerFactory.getLogger(ItemRepository.class);

    private final ObjectMapper objectMapper;
    private List<Item> items = new ArrayList<>();
    private Map<String, Item> itemsById = new HashMap<>();

    public ItemRepository() {
        this.objectMapper = JsonMapper.builder().build();
    }

    public ItemRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/items.json")) {
            if (is == null) {
                throw new IllegalStateException("Could not find /data/items.json in classpath");
            }
            List<Item> loadedItems = objectMapper.readValue(is, new TypeReference<List<Item>>() {});
            this.items = Collections.unmodifiableList(loadedItems);
            Map<String, Item> map = new HashMap<>();
            for (Item item : loadedItems) {
                map.put(item.getId(), item);
            }
            this.itemsById = Collections.unmodifiableMap(map);

            long weapons = loadedItems.stream().filter(i -> i.getCategory() == ItemCategory.WEAPON || i.getCategory() == ItemCategory.DEMONIC_WEAPON || i.getCategory() == ItemCategory.MELEE_WEAPON).count();
            long spells = loadedItems.stream().filter(i -> i.getCategory() == ItemCategory.LIGHT_SPELL || i.getCategory() == ItemCategory.HEAVY_SPELL).count();
            long magicalItems = loadedItems.stream().filter(i -> i.getCategory() == ItemCategory.RELIC || i.getCategory() == ItemCategory.FETISH || i.getCategory() == ItemCategory.RING).count();
            long beads = loadedItems.stream().filter(i -> i.getCategory() == ItemCategory.BEAD).count();

            logger.info("Loaded {} items from /data/items.json ({} weapons, {} spells, {} magical items, {} beads)",
                    loadedItems.size(), weapons, spells, magicalItems, beads);
        } catch (Exception e) {
            logger.error("Failed to load /data/items.json", e);
            throw new RuntimeException("Failed to load /data/items.json", e);
        }
    }

    public List<Item> findAll() {
        return items;
    }

    public Optional<Item> findById(String id) {
        return Optional.ofNullable(itemsById.get(id));
    }

    public List<Item> findByCategory(ItemCategory category) {
        if (category == null) {
            return items;
        }
        return items.stream()
                .filter(i -> i.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Item> search(ItemCategory category, Element element, String query) {
        return search(category, element, false, query, "category");
    }

    public List<Item> search(ItemCategory category, Element element, Boolean noElement, String query, String sort) {
        List<Item> result = items.stream()
                .filter(i -> category == null || i.getCategory() == category)
                .filter(i -> {
                    if (Boolean.TRUE.equals(noElement)) {
                        return i.getElement() == null;
                    }
                    return element == null || i.getElement() == element;
                })
                .filter(i -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.trim().toLowerCase();
                    return (i.getName() != null && i.getName().toLowerCase().contains(q))
                            || (i.getDescription() != null && i.getDescription().toLowerCase().contains(q));
                })
                .collect(Collectors.toList());

        if (sort == null || sort.equals("category")) {
            result.sort(Comparator.comparingInt((Item i) -> i.getCategory().ordinal())
                    .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("name-asc".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("name-desc".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("element-asc".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing((Item i) -> i.getElement() != null ? i.getElement().name() : "ZZZ")
                    .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("element-desc".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing((Item i) -> i.getElement() != null ? i.getElement().name() : "")
                    .reversed()
                    .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        }

        return result;
    }

    public List<Bead> findEligibleBeads(Map<String, Integer> userStats) {
        Map<String, Integer> stats = userStats != null ? userStats : Collections.emptyMap();
        return items.stream()
                .filter(i -> i instanceof Bead)
                .map(i -> (Bead) i)
                .filter(bead -> {
                    for (BeadRequirement req : bead.getRequirements()) {
                        int userVal = stats.getOrDefault(req.stat(), 0);
                        if (userVal < req.value()) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
