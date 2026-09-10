package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyRepositoryTest {

    private EnemyRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EnemyRepository();
        repository.init();
    }

    @Test
    void shouldLoadEnemiesFromClasspath() {
        assertThat(repository.findAll()).hasSizeGreaterThanOrEqualTo(50);
        assertThat(repository.getAllRanks()).isNotEmpty();
        assertThat(repository.getAllLocations()).isNotEmpty();
    }

    @Test
    void shouldFindEnemyById() {
        Optional<Enemy> anointer = repository.findById("enemy-anointer");
        assertThat(anointer).isPresent();
        assertThat(anointer.get().getName()).isEqualTo("Anointer");
        assertThat(anointer.get().getRank()).isEqualTo("Faithful");
        assertThat(anointer.get().getGnosis()).isEqualTo(3);
        assertThat(anointer.get().getDecayResistance()).isEqualTo(-100);
        assertThat(anointer.get().getFireResistance()).isEqualTo(75);
    }

    @Test
    void shouldFilterByLocationExactlySoRegionsAndVaultsStaySeparate() {
        List<Enemy> outskirts = repository.search(null, null, "Outskirts", "name");

        assertThat(outskirts).isNotEmpty();
        assertThat(outskirts).allMatch(e -> e.getLocations().contains("Outskirts"));
        // Bladesman is only in "Outskirts Vault"; a substring match would wrongly include it.
        assertThat(outskirts).noneMatch(e -> "Bladesman".equals(e.getName()));
    }

    @Test
    void shouldSearchEnemiesByLocationAndAttack() {
        assertThat(repository.search("vault", null, null, "name")).isNotEmpty();
        assertThat(repository.search("4 - 35", null, null, "name"))
                .isNotEmpty()
                .allMatch(e -> e.getDamage() != null && e.getDamage().contains("4 - 35"));
    }

    @Test
    void shouldGroupLocationsIntoRegionsVaultsAndSummons() {
        Map<String, List<EnemyRepository.LocationOption>> groups = repository.getLocationGroups();

        assertThat(groups.keySet()).containsExactly("Regions", "Vaults", "Summoned");

        assertThat(groups.get("Regions").stream().map(EnemyRepository.LocationOption::value))
                .contains("Irongate Castle", "Marshland")
                .noneMatch(value -> value.endsWith(" Vault") || value.startsWith("Summoned"));
        assertThat(groups.get("Vaults").stream().map(EnemyRepository.LocationOption::value))
                .isNotEmpty()
                .allMatch(value -> value.endsWith(" Vault"));
        assertThat(groups.get("Summoned").stream().map(EnemyRepository.LocationOption::value))
                .contains("Summoned by Calamity")
                .allMatch(value -> value.startsWith("Summoned by "));
        assertThat(groups.get("Summoned").stream().map(EnemyRepository.LocationOption::label))
                .contains("Calamity")
                .noneMatch(label -> label.startsWith("Summoned"));

        // Every location is offered exactly once across the groups.
        assertThat(groups.values().stream().flatMap(List::stream).map(EnemyRepository.LocationOption::value).toList())
                .containsExactlyInAnyOrderElementsOf(repository.getAllLocations());
    }

    @Test
    void shouldFilterByRank() {
        List<Enemy> faithful = repository.search(null, "Faithful", null, "name");
        assertThat(faithful).isNotEmpty();
        assertThat(faithful).allMatch(e -> "Faithful".equalsIgnoreCase(e.getRank()));
    }

    @Test
    void shouldSortByHealthDescending() {
        List<Enemy> sorted = repository.search(null, null, null, "health");
        assertThat(sorted).isNotEmpty();
        for (int i = 0; i < sorted.size() - 1; i++) {
            int h1 = sorted.get(i).getHealth() != null ? sorted.get(i).getHealth() : 0;
            int h2 = sorted.get(i + 1).getHealth() != null ? sorted.get(i + 1).getHealth() : 0;
            assertThat(h1).isGreaterThanOrEqualTo(h2);
        }
    }
}
