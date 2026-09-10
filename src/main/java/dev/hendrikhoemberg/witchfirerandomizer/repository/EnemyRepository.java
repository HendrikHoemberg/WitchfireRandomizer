package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.Enemy;
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
public class EnemyRepository {

    private static final Logger logger = LoggerFactory.getLogger(EnemyRepository.class);

    private final ObjectMapper objectMapper;
    private List<Enemy> enemyList = Collections.emptyList();
    private Map<String, Enemy> enemyById = Collections.emptyMap();

    public EnemyRepository() {
        this.objectMapper = JsonMapper.builder().build();
    }

    public EnemyRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/enemies.json")) {
            if (is != null) {
                List<Enemy> loaded = objectMapper.readValue(is, new TypeReference<List<Enemy>>() {});
                this.enemyList = Collections.unmodifiableList(loaded);
                Map<String, Enemy> map = new HashMap<>();
                for (Enemy e : loaded) {
                    map.put(e.getId(), e);
                }
                this.enemyById = Collections.unmodifiableMap(map);
                logger.info("Loaded {} enemies from /data/enemies.json", enemyList.size());
            } else {
                logger.warn("Could not find /data/enemies.json in classpath");
            }
        } catch (Exception e) {
            logger.error("Failed to load /data/enemies.json", e);
        }
    }

    public List<Enemy> findAll() {
        return enemyList;
    }

    public Optional<Enemy> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(enemyById.get(id));
    }

    public List<Enemy> search(String query, Integer gnosis, String rank, String location, String sort) {
        List<Enemy> filtered = enemyList.stream()
                .filter(e -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.trim().toLowerCase();
                        boolean matchesName = e.getName() != null && e.getName().toLowerCase().contains(q);
                        boolean matchesDesc = e.getDescription() != null && e.getDescription().toLowerCase().contains(q);
                        if (!matchesName && !matchesDesc) {
                            return false;
                        }
                    }
                    if (gnosis != null) {
                        if (e.getGnosis() == null || !e.getGnosis().equals(gnosis)) {
                            return false;
                        }
                    }
                    if (rank != null && !rank.isBlank()) {
                        if (e.getRank() == null || !e.getRank().equalsIgnoreCase(rank.trim())) {
                            return false;
                        }
                    }
                    if (location != null && !location.isBlank()) {
                        String locLower = location.trim().toLowerCase();
                        boolean matchesLoc = e.getLocations().stream()
                                .anyMatch(l -> l.toLowerCase().contains(locLower));
                        if (!matchesLoc) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        String sortKey = sort != null ? sort.trim().toLowerCase() : "name";
        switch (sortKey) {
            case "health" -> filtered.sort((a, b) -> {
                int h1 = a.getHealth() != null ? a.getHealth() : 0;
                int h2 = b.getHealth() != null ? b.getHealth() : 0;
                return Integer.compare(h2, h1); // descending
            });
            case "gnosis" -> filtered.sort((a, b) -> {
                int g1 = a.getGnosis() != null ? a.getGnosis() : 0;
                int g2 = b.getGnosis() != null ? b.getGnosis() : 0;
                return Integer.compare(g1, g2); // ascending
            });
            case "name" -> filtered.sort(Comparator.comparing(e -> e.getName() != null ? e.getName() : ""));
            default -> filtered.sort(Comparator.comparing(e -> e.getName() != null ? e.getName() : ""));
        }

        return filtered;
    }

    public List<String> getAllLocations() {
        return enemyList.stream()
                .flatMap(e -> e.getLocations().stream())
                .filter(l -> l != null && !l.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getAllRanks() {
        return enemyList.stream()
                .map(Enemy::getRank)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
