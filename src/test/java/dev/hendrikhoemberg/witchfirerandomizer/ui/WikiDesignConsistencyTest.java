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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

    private static String readTemplate(String path) throws IOException {
        return Files.readString(Path.of("src/main/resources/templates", path), StandardCharsets.UTF_8);
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
        for (String route : new String[]{"/wiki", "/wiki/arcana", "/wiki/prophecies", "/wiki/bestiary"}) {
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
                .andExpect(content().string(containsString("wiki-select")))
                .andExpect(content().string(containsString("All Categories")));
    }

    @Test
    void shouldStyleEveryWikiDropdownWithTheSharedSelectClass() throws Exception {
        String rule = cssRule(mainCss(), ".wiki-select");
        assertThat(rule).contains("appearance: none");
        assertThat(rule).contains("background-color: #201d19");

        for (String template : List.of("wiki/bestiary.html", "wiki/prophecies.html",
                "wiki/arcana.html", "wiki/index.html")) {
            assertThat(readTemplate(template))
                    .as("%s should use the shared select class", template)
                    .doesNotContain("custom-select")
                    .doesNotContain("wiki-sort-select");
        }
    }

    @Test
    void shouldGroupBestiaryLocationsForTheDropdown() throws Exception {
        mockMvc.perform(get("/wiki/bestiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<optgroup label=\"Regions\"")))
                .andExpect(content().string(containsString("<optgroup label=\"Vaults\"")))
                .andExpect(content().string(containsString("<optgroup label=\"Summoned\"")))
                .andExpect(content().string(containsString(">Calamity<")))
                .andExpect(content().string(not(containsString("Gnosis"))))
                .andExpect(content().string(not(containsString("selectedGnosis"))));
    }

    @Test
    void shouldRefreshTheResultCounterWithTheSwappedFragment() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("results-counter")));

        mockMvc.perform(get("/wiki/arcana/cards"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("results-counter")));

        mockMvc.perform(get("/wiki/prophecies/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("results-counter")));

        mockMvc.perform(get("/wiki/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("results-counter")));

        // The counter must live inside the swapped container, not beside it.
        String page = mockMvc.perform(get("/wiki/bestiary"))
                .andReturn().getResponse().getContentAsString();
        assertThat(page.indexOf("results-counter"))
                .isGreaterThan(page.indexOf("id=\"bestiary-enemies-grid\""));
    }

    @Test
    void shouldKeepTheEquipmentSortControlInsideTheFilterBox() throws Exception {
        String page = mockMvc.perform(get("/wiki"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page.indexOf("wiki-filter-box")).isLessThan(page.indexOf("id=\"sort-select\""));
        assertThat(page.indexOf("id=\"sort-select\"")).isLessThan(page.indexOf("id=\"wiki-items-grid\""));

        // …and the swapped fragment must not carry it, or every swap would reset it.
        mockMvc.perform(get("/wiki/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sort-select"))));
    }

    @Test
    void shouldDefineOpaqueModalBackgroundToken() throws Exception {
        String css = mainCss();
        assertThat(css).contains("--wf-modal-bg:");
        assertThat(css).contains("background: var(--wf-modal-bg);");
    }

    @Test
    void shouldNotRenderZeroPercentResistancesInEnemyModal() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemy/enemy-anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fire")))
                .andExpect(content().string(containsString("Freeze")))
                .andExpect(content().string(containsString("Decay")))
                .andExpect(content().string(not(containsString(">Shock<"))))
                .andExpect(content().string(not(containsString("Neutral"))));
    }

    @Test
    void shouldStyleEquipmentInOneColumnAndDarkGlassMysterium() throws Exception {
        String css = mainCss();
        String listRule = cssRule(css, ".wiki-cards-list");
        assertThat(listRule).doesNotContain("repeat(2, 1fr)");

        String mystRule = cssRule(css, ".wiki-mysterium-level-header");
        assertThat(mystRule).doesNotContain("background-color: var(--wf-gold-accent)");
        assertThat(mystRule).contains("background: rgba(0, 0, 0,");
    }

    @Test
    void shouldRenderSelfContainedArcanaGridWithoutModalTrigger() throws Exception {
        mockMvc.perform(get("/wiki/arcana"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("arcana-grid")))
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(not(containsString("openArcanaModal"))));
    }

    @Test
    void shouldStyleBestiaryInTwoColumnGridAndOmitPlaceholderText() throws Exception {
        String css = mainCss();
        String bestiaryGridRule = cssRule(css, ".bestiary-grid");
        assertThat(bestiaryGridRule).contains("repeat(2, 1fr)");

        mockMvc.perform(get("/wiki/bestiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("A denizen of the Witch domain."))))
                .andExpect(content().string(not(containsString("A denizen under the Witch command."))))
                .andExpect(content().string(containsString("enemy-card")))
                .andExpect(content().string(not(containsString("openEnemyModal"))));
    }

    @Test
    void shouldRenderALabeledCombatStatBlockOnEnemyCards() throws Exception {
        mockMvc.perform(get("/wiki/bestiary/enemies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enemy-stat-grid")))
                .andExpect(content().string(containsString("enemy-stat-label")))
                .andExpect(content().string(containsString(">Health<")))
                .andExpect(content().string(containsString(">Attack<")))
                .andExpect(content().string(not(containsString("enemy-combat-stats-row"))));
    }

    @Test
    void shouldOnlyReferenceImageFilesThatExist() throws Exception {
        // Catches case mismatches and stale extensions: the pages render, but the <img> silently
        // 404s. Nothing else in the suite fetches assets, so this is the only guard.
        Path staticRoot = Path.of("src/main/resources/static");
        Set<String> referenced = new TreeSet<>();

        for (String route : new String[]{"/", "/wiki", "/wiki/arcana", "/wiki/prophecies", "/wiki/bestiary"}) {
            String html = mockMvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            Matcher matcher = Pattern.compile("(?:src|href)=\"(/images/[^\"]+)\"").matcher(html);
            while (matcher.find()) {
                referenced.add(matcher.group(1));
            }
        }

        Matcher cssUrls = Pattern.compile("url\\('(/images/[^']+)'\\)").matcher(mainCss());
        while (cssUrls.find()) {
            referenced.add(cssUrls.group(1));
        }

        assertThat(referenced).as("pages should actually reference images").isNotEmpty();

        List<String> missing = referenced.stream()
                .filter(url -> !Files.exists(staticRoot.resolve(url.substring(1))))
                .toList();

        assertThat(missing).as("every referenced image must exist under static/").isEmpty();
    }

    @Test
    void shouldRenderEnemyAffinitiesAsALedgerInsteadOfPills() throws Exception {
        // Anointer resists Fire/Freeze/Air/Stagger and is vulnerable to Decay.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enemy-affinity-ledger")))
                .andExpect(content().string(containsString("enemy-affinity-entry")))
                .andExpect(content().string(containsString("enemy-affinity-track")))
                .andExpect(content().string(containsString("enemy-affinity-fill")))
                .andExpect(content().string(containsString(">Resists<")))
                .andExpect(content().string(containsString(">Vulnerable<")))
                .andExpect(content().string(not(containsString("affinity-pill"))))
                .andExpect(content().string(not(containsString("enemy-affinity-strip"))));
    }

    @Test
    void shouldLeadWithVulnerabilitiesBeforeResistances() throws Exception {
        String html = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html.indexOf(">Vulnerable<")).isNotNegative();
        assertThat(html.indexOf(">Vulnerable<")).isLessThan(html.indexOf(">Resists<"));
    }

    @Test
    void shouldEncodeAffinityMagnitudeInTheLedgerBar() throws Exception {
        String html = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Fire +75% and Decay -100% straight from the scraped data.
        assertThat(html).contains("width:75%");
        assertThat(html).contains("width:100%");
        assertThat(html).contains("+75%");
        assertThat(html).contains("-100%");
    }

    @Test
    void shouldGiveEachAffinityRowAnElementIdentityDot() throws Exception {
        String html = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("enemy-affinity-dot");
        assertThat(html).contains("#e0705c"); // Fire
        assertThat(html).contains("#84b06a"); // Decay
    }

    @Test
    void shouldRenderTheEnemyPortraitPlateBesideTheDossier() throws Exception {
        String cardRule = cssRule(mainCss(), ".enemy-card");
        assertThat(cardRule).contains("grid-template-columns");

        mockMvc.perform(get("/wiki/bestiary/enemies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enemy-card-portrait")))
                .andExpect(content().string(containsString("enemy-card-dossier")));
    }

    @Test
    void shouldUseDedicatedAffinityTokensInsteadOfAdHocColours() throws Exception {
        String css = mainCss();
        assertThat(css).contains("--wf-resist:");
        assertThat(css).contains("--wf-vulnerable:");
        assertThat(cssRule(css, ".enemy-affinity-fill")).contains("var(--wf-resist)");
        assertThat(cssRule(css, ".enemy-affinity-fill.is-vulnerable")).contains("var(--wf-vulnerable)");
    }

    @Test
    void shouldRenderAnExplicitEmptyStateForEnemiesWithoutAffinities() throws Exception {
        // Assassin has no elemental resistances and no vulnerabilities.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Assassin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No elemental resistances or weaknesses.")));
    }

    @Test
    void shouldRenderEveryTerritoryWithAnExpandToggle() throws Exception {
        // Blunderbusser lists 7 territories: all 7 chips are in the DOM, the last 4 start hidden.
        String card = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Blunderbusser"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(card).contains("x-data=\"{ expanded: false }\"");
        assertThat(card).contains(">Outskirts Vault<");
        assertThat(card).contains(">Velmorne Vault<");
        assertThat(card).contains(">Witch Mountain<");
        assertThat(card).contains("location-chip-more");
        assertThat(card).contains("+4 more");
        assertThat(card).contains("expanded = !expanded");
        // The hidden chips carry the toggle binding; the first three do not.
        assertThat(card.split("x-show=\"expanded\"", -1).length - 1).isEqualTo(4);

        // Anointer lists a single territory: no toggle.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("location-chip-more"))));

        // The toggle must read as a control, not as one more territory chip.
        String toggleRule = cssRule(mainCss(), "button.location-chip-more");
        assertThat(toggleRule).contains("cursor: pointer");
        assertThat(toggleRule).contains("color: var(--wf-gold)");
        assertThat(toggleRule).contains("border-style: dashed");
    }

    @Test
    void shouldNotDuplicateTheRankTagOnUniqueEnemies() throws Exception {
        // "Sepulcher" is both the enemy name and its rank label.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Sepulcher"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("enemy-rank-tag"))));

        // Regular ranks still render their tag.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enemy-rank-tag")))
                .andExpect(content().string(containsString(">Faithful<")));
    }

    @Test
    void shouldGiveTheEnemyRankTheProminentTitleSlotLeftByGnosis() throws Exception {
        String card = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(card).contains("enemy-card-title-row");
        assertThat(card).contains("enemy-rank-tag");
        assertThat(card).contains(">Faithful<");
        assertThat(card).doesNotContain("gnosis-badge");
        // The badge sits in the title row, ahead of the stat block.
        assertThat(card.indexOf("enemy-card-title-row")).isLessThan(card.indexOf("enemy-rank-tag"));
        assertThat(card.indexOf("enemy-rank-tag")).isLessThan(card.indexOf("enemy-stat-grid"));
    }

    @Test
    void shouldNotMentionGnosisAnywhereInTheBestiary() throws Exception {
        String page = mockMvc.perform(get("/wiki/bestiary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page.toLowerCase()).doesNotContain("gnosis");
        assertThat(mainCss()).doesNotContain(".gnosis-badge");
    }

    @Test
    void shouldGiveAlternateFormsTheirOwnLabelledSection() throws Exception {
        // Axeman exists as Elite and Ascended.
        String card = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Axeman"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(card).contains("enemy-forms-row");
        assertThat(card).contains("enemy-forms-label");
        assertThat(card).contains("Forms");
        assertThat(card).contains("enemy-form-chip");
        assertThat(card).contains(">Elite<");
        assertThat(card).contains(">Ascended<");
        assertThat(card).doesNotContain("enemy-variant-tag");
    }

    @Test
    void shouldHideTheFormsSectionForEnemiesWithoutAlternateForms() throws Exception {
        // Anointer has no Elite/Ascended forms (Assassin does, despite the name).
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("enemy-forms-row"))));

        // The styling for a section that can be absent must still exist.
        assertThat(cssRule(mainCss(), ".enemy-form-chip")).contains("text-transform: uppercase");
    }

    @Test
    void shouldRenderEquipmentCardsWithPortraitPlateAndDossier() throws Exception {
        String css = mainCss();
        String cardRule = cssRule(css, ".wiki-item-card");
        assertThat(cardRule).contains("grid-template-columns");

        mockMvc.perform(get("/wiki/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wiki-item-portrait-plate")))
                .andExpect(content().string(containsString("wiki-item-dossier")))
                .andExpect(content().string(containsString("equipment-category-tag")));
    }

    @Test
    void shouldRenderEquipmentStatsInBestiaryStyleGrid() throws Exception {
        mockMvc.perform(get("/wiki/items").param("search", "All-Seeing Eye"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("equipment-stat-grid")))
                .andExpect(content().string(containsString("equipment-stat-label")))
                .andExpect(content().string(containsString(">DAMAGE<")))
                .andExpect(content().string(containsString(">STUN<")));
    }

    @Test
    void shouldStackEquipmentCardsInRandomizerModal() throws Exception {
        String css = mainCss();
        String modalCardRule = cssRule(css, ".detail-modal-box .wiki-item-card");
        assertThat(modalCardRule).contains("grid-template-columns: 1fr");

        String modalPlateRule = cssRule(css, ".detail-modal-box .wiki-item-portrait-plate");
        assertThat(modalPlateRule).contains("border-right: none");
    }

    @Test
    void shouldPreserveMysteriumEffectAndRequirementStyling() throws Exception {
        mockMvc.perform(get("/wiki/items").param("search", "All-Seeing Eye"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wiki-mysterium-original-content")))
                .andExpect(content().string(containsString("Effect:")))
                .andExpect(content().string(containsString("Requirements:")));
    }
}


