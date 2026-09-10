package dev.hendrikhoemberg.witchfirerandomizer.repository;

import dev.hendrikhoemberg.witchfirerandomizer.model.Arcana;
import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
import dev.hendrikhoemberg.witchfirerandomizer.model.Prophecy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ArcanaRepositoryTest {

    private ArcanaRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ArcanaRepository();
        repository.init();
    }

    @Test
    void shouldLoadArcanaAndPropheciesFromClasspath() {
        assertThat(repository.findAll()).hasSizeGreaterThanOrEqualTo(100);
        assertThat(repository.findAllProphecies()).hasSizeGreaterThanOrEqualTo(15);
    }

    @Test
    void shouldFindArcanaById() {
        Optional<Arcana> accelerant = repository.findById("arcana-accelerant");
        assertThat(accelerant).isPresent();
        assertThat(accelerant.get().getName()).isEqualTo("Accelerant");
        assertThat(accelerant.get().getElement()).isEqualTo(Element.Fire);
    }

    @Test
    void shouldSearchArcanaByKeywordAndElement() {
        List<Arcana> results = repository.search("burning", null, Element.Fire);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(a -> a.getElement() == Element.Fire);
    }

    @Test
    void shouldLinkProphecyToAssociatedArcana() {
        Optional<Prophecy> fireProphecy = repository.findProphecyById("prophecy-prophecy-of-fire");
        assertThat(fireProphecy).isPresent();
        List<Arcana> linked = repository.findArcanaByProphecy("prophecy-prophecy-of-fire");
        assertThat(linked).isNotEmpty();
        assertThat(linked).allMatch(a -> a.getProphecyTypes().contains("Fire Element"));
    }
}
