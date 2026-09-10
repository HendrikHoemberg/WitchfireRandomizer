package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.Arcana;
import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
import dev.hendrikhoemberg.witchfirerandomizer.model.Prophecy;
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
public class ArcanaRepository {

    private static final Logger logger = LoggerFactory.getLogger(ArcanaRepository.class);

    private final ObjectMapper objectMapper;
    private List<Arcana> arcanaList = Collections.emptyList();
    private Map<String, Arcana> arcanaById = Collections.emptyMap();

    private List<Prophecy> prophecyList = Collections.emptyList();
    private Map<String, Prophecy> propheciesById = Collections.emptyMap();

    public ArcanaRepository() {
        this.objectMapper = JsonMapper.builder().build();
    }

    public ArcanaRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/arcana.json")) {
            if (is != null) {
                List<Arcana> loaded = objectMapper.readValue(is, new TypeReference<List<Arcana>>() {});
                this.arcanaList = Collections.unmodifiableList(loaded);
                Map<String, Arcana> map = new HashMap<>();
                for (Arcana a : loaded) {
                    map.put(a.getId(), a);
                }
                this.arcanaById = Collections.unmodifiableMap(map);
            }
        } catch (Exception e) {
            logger.error("Failed to load /data/arcana.json", e);
        }

        try (InputStream is = getClass().getResourceAsStream("/data/prophecies.json")) {
            if (is != null) {
                List<Prophecy> loaded = objectMapper.readValue(is, new TypeReference<List<Prophecy>>() {});
                this.prophecyList = Collections.unmodifiableList(loaded);
                Map<String, Prophecy> map = new HashMap<>();
                for (Prophecy p : loaded) {
                    map.put(p.getId(), p);
                }
                this.propheciesById = Collections.unmodifiableMap(map);
            }
        } catch (Exception e) {
            logger.error("Failed to load /data/prophecies.json", e);
        }

        logger.info("Loaded {} Arcana cards and {} Prophecies", arcanaList.size(), prophecyList.size());
    }

    public List<Arcana> findAll() {
        return arcanaList;
    }

    public Optional<Arcana> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(arcanaById.get(id));
    }

    public List<Prophecy> findAllProphecies() {
        return prophecyList;
    }

    public Optional<Prophecy> findProphecyById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(propheciesById.get(id));
    }

    public List<Arcana> findArcanaByProphecy(String prophecyId) {
        Prophecy prophecy = propheciesById.get(prophecyId);
        if (prophecy == null || prophecy.getArcanaType() == null || prophecy.getArcanaType().isBlank()) {
            return Collections.emptyList();
        }
        String targetType = prophecy.getArcanaType().toLowerCase();
        return arcanaList.stream()
                .filter(a -> a.getProphecyTypes().stream()
                        .anyMatch(t -> t.equalsIgnoreCase(targetType) || t.toLowerCase().contains(targetType)))
                .toList();
    }

    public List<Arcana> search(String query, String prophecyType, Element element) {
        return arcanaList.stream()
                .filter(a -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.trim().toLowerCase();
                        boolean matchesName = a.getName() != null && a.getName().toLowerCase().contains(q);
                        boolean matchesDesc = a.getDescription() != null && a.getDescription().toLowerCase().contains(q);
                        boolean matchesEffect = a.getEffects() != null && a.getEffects().toLowerCase().contains(q);
                        if (!matchesName && !matchesDesc && !matchesEffect) {
                            return false;
                        }
                    }
                    if (prophecyType != null && !prophecyType.isBlank()) {
                        boolean matchesType = a.getProphecyTypes().stream()
                                .anyMatch(t -> t.equalsIgnoreCase(prophecyType.trim()));
                        if (!matchesType) return false;
                    }
                    if (element != null) {
                        if (a.getElement() != element) return false;
                    }
                    return true;
                })
                .toList();
    }

    public List<String> getAllProphecyTypes() {
        return arcanaList.stream()
                .flatMap(a -> a.getProphecyTypes().stream())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getAllLocations() {
        return prophecyList.stream()
                .map(Prophecy::getLocation)
                .filter(l -> l != null && !l.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
