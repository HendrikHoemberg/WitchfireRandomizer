package dev.hendrikhoemberg.witchfirerandomizer.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyTest {

    private static Enemy withVariants(String variants) {
        Enemy enemy = new Enemy();
        enemy.setVariants(variants);
        return enemy;
    }

    @Test
    void shouldSplitAlternateFormsIntoSeparateLabels() {
        assertThat(withVariants("Elite / Ascended").getVariantForms())
                .containsExactly("Elite", "Ascended");
    }

    @Test
    void shouldReturnASingleFormWhenThereIsNothingToSplit() {
        assertThat(withVariants("Elite").getVariantForms()).containsExactly("Elite");
    }

    @Test
    void shouldReturnNoFormsForBlankOrMissingVariants() {
        assertThat(withVariants("").getVariantForms()).isEmpty();
        assertThat(withVariants("   ").getVariantForms()).isEmpty();
        assertThat(withVariants(null).getVariantForms()).isEmpty();
    }
}
