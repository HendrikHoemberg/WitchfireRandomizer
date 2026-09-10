package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void shouldFilterByGnosisLevel() {
        List<Enemy> gnosis3 = repository.search(null, 3, null, null, "name");
        assertThat(gnosis3).isNotEmpty();
        assertThat(gnosis3).allMatch(e -> e.getGnosis() != null && e.getGnosis() == 3);
    }

    @Test
    void shouldFilterByRank() {
        List<Enemy> faithful = repository.search(null, null, "Faithful", null, "name");
        assertThat(faithful).isNotEmpty();
        assertThat(faithful).allMatch(e -> "Faithful".equalsIgnoreCase(e.getRank()));
    }

    @Test
    void shouldSortByHealthDescending() {
        List<Enemy> sorted = repository.search(null, null, null, null, "health");
        assertThat(sorted).isNotEmpty();
        for (int i = 0; i < sorted.size() - 1; i++) {
            int h1 = sorted.get(i).getHealth() != null ? sorted.get(i).getHealth() : 0;
            int h2 = sorted.get(i + 1).getHealth() != null ? sorted.get(i + 1).getHealth() : 0;
            assertThat(h1).isGreaterThanOrEqualTo(h2);
        }
    }
}
