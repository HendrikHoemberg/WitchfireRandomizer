package dev.hendrikhoemberg.witchfirerandomizer.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the visual consistency of the wiki pages against the rest of the app.
 *
 * <p>These tests exist because the Arcana/Prophecies/Bestiary work introduced a parallel design
 * language (a second gold, an undefined serif font, opaque card surfaces) next to the app's own
 * token system. They fail on the pre-harmonization tree on purpose.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WikiDesignConsistencyTest {

    private static final Path MAIN_CSS = Path.of("src/main/resources/static/css/main.css");

    @Autowired
    private MockMvc mockMvc;

    private static String mainCss() throws IOException {
        return Files.readString(MAIN_CSS, StandardCharsets.UTF_8);
    }

    private static Set<String> matches(String css, Pattern pattern) {
        Set<String> found = new HashSet<>();
        Matcher matcher = pattern.matcher(css);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    @Test
    void shouldDefineEveryCssVariableItUses() throws Exception {
        String css = mainCss();
        Set<String> used = matches(css, Pattern.compile("var\\((--[a-zA-Z0-9-]+)"));
        Set<String> defined = matches(css, Pattern.compile("(--[a-zA-Z0-9-]+)\\s*:"));

        assertThat(used).isSubsetOf(defined);
    }

    @Test
    void shouldUseOnlyTheSingleAppGold() throws Exception {
        String css = mainCss();

        assertThat(css).doesNotContain("212, 175, 55");
        assertThat(css).doesNotContain("#d4af37");
    }

    @Test
    void shouldNotReferenceAnUndefinedGothicFont() throws Exception {
        assertThat(mainCss()).doesNotContain("font-gothic");
    }

    @Test
    void shouldNotOverrideTheSharedGlassSurfaceOnWikiCards() throws Exception {
        String css = mainCss();

        assertThat(css).doesNotContain("#141210");
        assertThat(css).doesNotContain("#0a0908");
    }

    /** Returns the declaration block of the first rule whose selector matches exactly. */
    private static String cssRule(String css, String selector) {
        Matcher matcher = Pattern
                .compile(Pattern.quote(selector) + "\\s*\\{([^}]*)\\}", Pattern.DOTALL)
                .matcher(css);
        assertThat(matcher.find()).as("CSS rule %s should exist", selector).isTrue();
        return matcher.group(1);
    }

    @Test
    void shouldNotRestyleTheSharedModalBoxForWikiOnly() throws Exception {
        // The shared modal box is used by the randomizer too, so it keeps the pre-overhaul
        // geometry and takes its surface from .texture-box.
        String rule = cssRule(mainCss(), ".detail-modal-box");

        assertThat(rule).doesNotContain("max-width: 620px");
        assertThat(rule).doesNotContain("background-color");
        assertThat(rule).doesNotContain("border:");
    }

    @Test
    void shouldNotUseAFullRoundPillBarForWikiSubNavigation() throws Exception {
        String css = mainCss();

        assertThat(css).doesNotContain("border-radius: 9999px;\n  padding: 0.35rem;");
        assertThat(css).doesNotContain("box-shadow: 0 0 10px rgba(var(--wf-gold-rgb), 0.2);");
    }

    @Test
    void shouldRenderTheSharedFooterCreditOnWikiPages() throws Exception {
        for (String route : new String[]{"/wiki", "/wiki/arcana", "/wiki/bestiary"}) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Gregory Pedzinski")));
        }
    }

    @Test
    void shouldNotMentionTheRemovedRankFilter() throws Exception {
        // The empty state only renders for a query without matches, so ask for one.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "zzz-no-such-enemy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No Enemies Found")))
                .andExpect(content().string(not(containsString("rank filter"))));
    }

    @Test
    void shouldMarkTheActiveWikiTabAndRenderAllTabs() throws Exception {
        mockMvc.perform(get("/wiki/arcana"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wiki-subnav-tabs")))
                .andExpect(content().string(containsString("wiki-tab-link active")));
    }

    @Test
    void shouldNotUseSaturatedNeonColorsInCss() throws Exception {
        String css = mainCss();
        assertThat(css).doesNotContain("#34d399");
        assertThat(css).doesNotContain("16, 185, 129");
        assertThat(css).doesNotContain("#c084fc");
    }

    @Test
    void shouldNotRenderResistanceBadgesOnEnemyCards() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemies"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("resistance-badges-container"))));
    }

    @Test
    void shouldNotRenderVerboseLocationStringsOnEnemyCards() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemies"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(" • Irongate Castle"))));
    }

    @Test
    void shouldRenderDropdownForArcanaProphecyCategories() throws Exception {
        mockMvc.perform(get("/wiki/arcana"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("custom-select")))
                .andExpect(content().string(containsString("All Categories")));
    }
}

