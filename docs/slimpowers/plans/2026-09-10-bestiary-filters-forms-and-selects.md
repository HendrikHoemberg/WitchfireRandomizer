# Bestiary Filters, Alternate Forms & Shared Selects — Implementation Plan

**Goal:** Remove Gnosis from the bestiary UI, make location the primary bestiary filter, give the enemy
rank prominence and alternate forms a section of their own, make every territory reachable, and give all
four wiki pages one styled dropdown.

**Architecture:** Server-rendered Thymeleaf fragments stay the source of truth; the repository gains the
two pieces of presentation-adjacent data the templates need (grouped location options, split form labels).
Alpine.js only handles the per-card territory expand toggle (local state, no requests). One new CSS class
(`.wiki-select`) replaces three ad-hoc select class names and the inline style Arcana carries today.

**Tech Stack:** Spring Boot 4.1 / Java 25, Thymeleaf, HTMX, Alpine.js, hand-written `main.css`.

**Spec:** User request of 2026-09-10 (items 1–5) plus approved mockups
`target/ui-review/mockup-forms.html` (Option A) and `target/ui-review/mockup-locations.html`
(Filter 3 + Territory T2).

## Global Constraints

- Verify with `./mvnw test`; single test with `./mvnw test -Dtest=Class#method`.
- Every production change is test-first: write the test, run it, **watch it fail**, then implement.
- Design tokens live in `:root`; no ad-hoc colours in rules. Gold = chrome/labels, bone = resistance,
  ember = vulnerability, red = danger only.
- No inline `style` attributes for the controls touched here — the shared CSS class carries the styling.
- `target/` is gitignored; mockups and screenshots live there.
- Do not touch `.mvn/wrapper/*` or generated build artifacts.

---

## Decisions already taken (from the approved mockups)

| Question | Choice |
|---|---|
| Alternate forms | **Option A** — a gold `Forms` label followed by bone chips (`ELITE`, `ASCENDED`) |
| Location filter | **Filter 3** — one styled dropdown with `Regions` / `Vaults` / `Summoned` optgroups |
| Territory overflow | **Territory T2** — first 3 chips + a `+N more` toggle that expands in place |
| Rank | Prominent badge in the slot the Gnosis badge used to occupy (top-right of the title row) |

## Files touched (map)

| File | Change |
|---|---|
| `src/main/java/.../model/Enemy.java` | add `getVariantForms()`; keep `gnosis` data field |
| `src/main/java/.../repository/EnemyRepository.java` | drop the `gnosis` filter, match locations exactly, search locations + attack, add `getLocationGroups()` + `LocationOption` |
| `src/main/java/.../controller/BestiaryWikiController.java` | drop the `gnosis` request param and `gnosisLevels` model attribute |
| `src/main/resources/templates/wiki/bestiary.html` | drop the Gnosis pill row; grouped location dropdown; sort moves into the filter box; counter leaves the page |
| `src/main/resources/templates/wiki/prophecies.html` | styled location dropdown |
| `src/main/resources/templates/wiki/arcana.html` | replace the inline-styled select with `.wiki-select` |
| `src/main/resources/templates/wiki/index.html` | Equipment sort control moves into the filter box |
| `src/main/resources/templates/wiki/fragments/enemy-grid.html` | no Gnosis badge; prominent rank; Forms section; territory toggle; header + counter |
| `src/main/resources/templates/wiki/fragments/item-grid.html` | sort control removed; header uses shared classes |
| `src/main/resources/templates/wiki/fragments/arcana-grid.html` | header + counter |
| `src/main/resources/templates/wiki/fragments/prophecy-list.html` | header + counter |
| `src/main/resources/templates/wiki/fragments/enemy-modal.html` | drop the Gnosis badge (unreachable dead template, kept in place) |
| `src/main/resources/static/css/main.css` | `.wiki-select`; `.enemy-rank-tag` restyled; `.enemy-forms-*`; territory toggle; delete `.gnosis-badge`, `.enemy-variant-tag`, `.enemy-tags-row` |
| `src/test/java/.../model/EnemyTest.java` | create |
| `src/test/java/.../repository/EnemyRepositoryTest.java` | update |
| `src/test/java/.../controller/BestiaryWikiControllerTest.java` | update |
| `src/test/java/.../ui/WikiDesignConsistencyTest.java` | update + add |

---

### Task 1: Split the alternate-forms string on the model

The raw data holds `variants` as one display string (`"Elite / Ascended"`, `"Elite"`, `""`). The card needs
individual labels, so the split belongs on the model, not in a Thymeleaf expression.

**Files:**
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Enemy.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/model/EnemyTest.java` (create)

**Interfaces:**
- Consumes: `Enemy(String variants)` (already present)
- Produces: `List<String> Enemy.getVariantForms()` — never null; empty when there are no alternate forms

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run the single test — verify it FAILS**

`./mvnw test -Dtest=EnemyTest` → expected: **compile error**, `getVariantForms()` does not exist.

- [ ] **Step 3: Write minimal implementation**

In `Enemy.java`, next to the existing `getResistances()` / `getVulnerabilities()` delegates:

```java
    /** Alternate forms this enemy also appears as ("Elite / Ascended" → ["Elite", "Ascended"]). */
    public List<String> getVariantForms() {
        if (variants == null || variants.isBlank()) {
            return List.of();
        }
        return Arrays.stream(variants.split("/"))
                .map(String::trim)
                .filter(form -> !form.isEmpty())
                .toList();
    }
```

Add `import java.util.Arrays;` if it is not already imported (`List` already is).

- [ ] **Step 4: Run the single test — verify it PASSES**

`./mvnw test -Dtest=EnemyTest` → expected: **3 tests, 0 failures**.

- [ ] **Step 5: Commit**

`git commit -m "feat(bestiary): expose enemy alternate forms as individual labels"`

---

### Task 2: Location becomes the real filter (repository + controller)

Three defects are fixed here, all of them in the path the new location filter relies on:

1. `getLocationGroups()` — the grouped options the dropdown needs. Grouping rule: a location ending in
   `" Vault"` is a **Vault**, one starting with `"Summoned by "` is a **Summon** (label without the
   prefix), everything else is a **Region**.
2. The location filter used `contains`, so choosing `Irongate Castle` also returned the 4 enemies that are
   only in `Irongate Castle Vault` (and `Outskirts` pulled in 13 Vault-only enemies). It becomes exact.
3. The search box promises “name, attack, or location” but only matched name + the always-empty
   `description`; it now matches name, attack range and location.

The `gnosis` filter parameter is deleted. `rank` stays as it is (unused by the UI, out of scope).

**Files:**
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/EnemyRepository.java`
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiController.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/EnemyRepositoryTest.java`

**Interfaces:**
- Consumes: `Enemy.getName()/getDamage()/getLocations()/getRank()/getHealth()`
- Produces:
  - `List<Enemy> EnemyRepository.search(String query, String rank, String location, String sort)` — the
    `gnosis` parameter is **gone**; callers pass `rank` as `null`
  - `Map<String, List<EnemyRepository.LocationOption>> EnemyRepository.getLocationGroups()` — insertion
    order `Regions`, `Vaults`, `Summoned`; groups with no members are absent
  - `record EnemyRepository.LocationOption(String value, String label)`

- [ ] **Step 1: Write the failing tests**

Replace `shouldFilterByGnosisLevel` in `EnemyRepositoryTest` and add the new cases:

```java
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
```

Fix the two existing call sites in the same file (they lose the `gnosis` argument):

```java
        List<Enemy> faithful = repository.search(null, "Faithful", null, "name");
        List<Enemy> sorted = repository.search(null, null, null, "health");
```

Add `import java.util.Map;` to the test.

- [ ] **Step 2: Run the tests — verify they FAIL**

`./mvnw test -Dtest=EnemyRepositoryTest` → expected: **compile error** (`search` arity,
`getLocationGroups`, `LocationOption` do not exist).

- [ ] **Step 3: Write minimal implementation**

In `EnemyRepository`, replace the `search` signature and its filter block:

```java
    public List<Enemy> search(String query, String rank, String location, String sort) {
        List<Enemy> filtered = enemyList.stream()
                .filter(e -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.trim().toLowerCase();
                        boolean matchesName = e.getName() != null && e.getName().toLowerCase().contains(q);
                        boolean matchesDamage = e.getDamage() != null && e.getDamage().toLowerCase().contains(q);
                        boolean matchesLocation = e.getLocations() != null && e.getLocations().stream()
                                .anyMatch(l -> l != null && l.toLowerCase().contains(q));
                        if (!matchesName && !matchesDamage && !matchesLocation) {
                            return false;
                        }
                    }
                    if (rank != null && !rank.isBlank()) {
                        if (e.getRank() == null || !e.getRank().equalsIgnoreCase(rank.trim())) {
                            return false;
                        }
                    }
                    if (location != null && !location.isBlank()) {
                        String wanted = location.trim();
                        if (e.getLocations() == null || e.getLocations().stream()
                                .noneMatch(l -> l != null && l.equalsIgnoreCase(wanted))) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
```

Delete the `case "gnosis" ->` branch from the sort switch (the sort option is removed in Task 5).

Add below `getAllLocations()`:

```java
    /** A dropdown entry: the full location (posted value) plus the label to show. */
    public record LocationOption(String value, String label) {
    }

    /** Locations grouped for the bestiary dropdown, in display order. */
    public Map<String, List<LocationOption>> getLocationGroups() {
        Map<String, List<LocationOption>> groups = new LinkedHashMap<>();
        List<LocationOption> regions = new ArrayList<>();
        List<LocationOption> vaults = new ArrayList<>();
        List<LocationOption> summoned = new ArrayList<>();

        for (String location : getAllLocations()) {
            if (location.endsWith(" Vault")) {
                vaults.add(new LocationOption(location, location));
            } else if (location.startsWith("Summoned by ")) {
                summoned.add(new LocationOption(location, location.substring("Summoned by ".length())));
            } else {
                regions.add(new LocationOption(location, location));
            }
        }

        if (!regions.isEmpty()) groups.put("Regions", regions);
        if (!vaults.isEmpty()) groups.put("Vaults", vaults);
        if (!summoned.isEmpty()) groups.put("Summoned", summoned);
        return groups;
    }
```

`getAllLocations()` already sorts alphabetically, so each group is sorted too. Add
`import java.util.LinkedHashMap;` (`java.util.*` is already imported in this file — check and leave as is).

In `BestiaryWikiController`, delete the `gnosis` request parameter, the `gnosisLevels` model attribute and
its `List` import if it becomes unused; both `search(...)` calls become
`enemyRepository.search(search, null, location, sort)`.

- [ ] **Step 4: Run the tests — verify they PASS**

`./mvnw test -Dtest=EnemyRepositoryTest` → expected: **7 tests, 0 failures**.

- [ ] **Step 5: Commit**

`git commit -m "feat(bestiary): filter enemies by exact location and group the location options"`

---

### Task 3: Prominent rank badge and a labelled Forms section on the card

Removes the Gnosis badge from the enemy card, promotes the rank into that slot, and gives alternate forms
their own labelled row (mockup Option A).

**Files:**
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Modify: `src/main/resources/templates/wiki/fragments/enemy-modal.html` (drop the same badge)
- Modify: `src/main/resources/static/css/main.css`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: `Enemy.getVariantForms()` (Task 1), `Enemy.getRank()`, `Enemy.getName()`
- Produces: card markup carrying `enemy-rank-tag` inside `.enemy-card-title-row`, and
  `.enemy-forms-row` → `.enemy-forms-label` + `.enemy-form-chip` per form

- [ ] **Step 1: Write the failing tests**

Add to `WikiDesignConsistencyTest`:

```java
    @Test
    void shouldGiveTheEnemyRankTheProminentTitleSlotLeftByGnosis() throws Exception {
        String card = mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(card).contains("enemy-card-title-row");
        assertThat(card).contains("enemy-rank-tag");
        assertThat(card).contains(">Faithful<");
        assertThat(card).doesNotContain("gnosis-badge");
        // the badge sits in the title row, not in a tag row of its own
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
        // Assassin has no Elite/Ascended forms.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Assassin"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("enemy-forms-row"))));

        // The CSS for a section that can be absent must still exist.
        assertThat(cssRule(mainCss(), ".enemy-form-chip")).contains("text-transform: uppercase");
    }
```

Remove the now-obsolete `shouldNotDuplicateTheRankTagOnUniqueEnemies` assertions about
`enemy-rank-tag` only if they no longer hold — they do still hold (Sepulcher suppresses it), so keep that
test as it is.

- [ ] **Step 2: Run the tests — verify they FAIL**

`./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: failures on the four new tests
(`enemy-forms-row` missing, `gnosis-badge` still present, `.gnosis-badge` still in the CSS).

- [ ] **Step 3: Write minimal implementation**

In `enemy-grid.html`, replace the header block (lines 22–41) with:

```html
        <div class="enemy-card-dossier">
            <!-- Header: name with the rank badge in the slot the Gnosis seal used to occupy -->
            <header class="enemy-card-header">
                <div class="enemy-card-heading">
                    <div class="enemy-card-title-row">
                        <h3 class="enemy-card-title" th:text="${enemy.name}">Enemy Name</h3>
                        <span th:if="${enemy.rank != null and !enemy.rank.isBlank() and !enemy.rank.equalsIgnoreCase(enemy.name)}"
                              class="enemy-rank-tag"
                              th:text="${enemy.rank}">Rank</span>
                    </div>
                </div>
            </header>
```

and insert the Forms row between `.enemy-stat-grid` and `.enemy-affinity-ledger`:

```html
            <!-- Alternate forms: this enemy also appears as these upgraded variants -->
            <div th:if="${!enemy.variantForms.isEmpty()}" class="enemy-forms-row">
                <span class="enemy-forms-label">Forms</span>
                <div class="enemy-forms-chips">
                    <span th:each="form : ${enemy.variantForms}" class="enemy-form-chip" th:text="${form}">Elite</span>
                </div>
            </div>
```

In `enemy-modal.html`, delete the line
`<span th:if="${enemy.gnosis != null}" class="gnosis-badge" th:text="'Gnosis ' + ${enemy.gnosis}">Gnosis 0</span>`
and drop `gnosis` from the comment above it.

In `main.css`, replace `.enemy-tags-row`, `.enemy-rank-tag`, `.enemy-variant-tag` and `.gnosis-badge`
(lines 1739–1770) with:

```css
/* Rank sits where the Gnosis seal used to: the one chrome badge on the card. */
.enemy-rank-tag {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  padding: 0.22rem 0.6rem;
  flex-shrink: 0;
  white-space: nowrap;
  background: rgba(var(--wf-gold-rgb), 0.12);
  border: 1px solid rgba(var(--wf-gold-rgb), 0.38);
  color: var(--wf-gold);
  border-radius: 0.25rem;
}

/* Alternate forms: labelled, so the chips cannot be mistaken for a second rank. */
.enemy-forms-row {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
  margin-bottom: 0.9rem;
}

.enemy-forms-label {
  flex: 0 0 4.75rem;
  padding-top: 0.2rem;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--wf-gold);
}

.enemy-forms-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.enemy-form-chip {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  padding: 0.2rem 0.55rem;
  border-radius: 0.25rem;
  color: var(--wf-resist-bright);
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(203, 195, 178, 0.35);
}
```

- [ ] **Step 4: Run the tests — verify they PASS**

`./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: all green.

- [ ] **Step 5: Commit**

`git commit -m "feat(bestiary): promote the enemy rank and give alternate forms their own section"`

---

### Task 4: Every territory reachable behind an expand toggle

**Files:**
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Modify: `src/main/resources/static/css/main.css`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: `Enemy.getLocations()`
- Produces: `.enemy-locations-row[x-data="{ expanded: false }"]` with **all** chips rendered, the 4th
  onwards carrying `x-show="expanded"`, plus a `.location-chip-more` button labelled `+N more`

- [ ] **Step 1: Write the failing test**

Replace `shouldCondenseEnemyTerritoriesWithAnOverflowChip` with:

```java
    @Test
    void shouldRenderEveryTerritoryWithAnExpandToggle() throws Exception {
        // Blunderbusser lists 7 territories: all 7 chips are rendered, the last 4 start hidden.
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

        assertThat(cssRule(mainCss(), "button.location-chip-more")).contains("cursor: pointer");
    }
```

- [ ] **Step 2: Run the test — verify it FAILS**

`./mvnw test -Dtest=WikiDesignConsistencyTest#shouldRenderEveryTerritoryWithAnExpandToggle` →
expected: FAIL — the card renders at most 3 chips and has no `x-data`/toggle binding.

- [ ] **Step 3: Write minimal implementation**

Replace the territory block in `enemy-grid.html`:

```html
            <!-- Territory: every location is in the DOM; the overflow ones expand in place. -->
            <div th:if="${enemy.locations != null and !enemy.locations.isEmpty()}"
                 class="enemy-locations-row"
                 x-data="{ expanded: false }"
                 th:with="hiddenCount=${enemy.locations.size() - 3}">
                <span class="enemy-locations-label">Territory</span>
                <div class="locations-chips-row">
                    <span th:each="loc, iter : ${enemy.locations}"
                          class="location-chip"
                          th:attr="x-show=${iter.index >= 3} ? 'expanded' : null"
                          th:text="${loc}">Irongate Castle</span>
                    <button th:if="${hiddenCount > 0}"
                            type="button"
                            class="location-chip location-chip-more"
                            th:data-more="|+${hiddenCount} more|"
                            th:text="|+${hiddenCount} more|"
                            @click="expanded = !expanded"
                            x-text="expanded ? 'Show fewer' : $el.dataset.more">+1 more</button>
                </div>
            </div>
```

In `main.css`, keep `.location-chip-more` and add the button reset right after it:

```css
button.location-chip-more {
  font-family: inherit;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.35);
}

button.location-chip-more:hover {
  background: rgba(var(--wf-gold-rgb), 0.16);
}
```

- [ ] **Step 4: Run the test — verify it PASSES**

`./mvnw test -Dtest=WikiDesignConsistencyTest#shouldRenderEveryTerritoryWithAnExpandToggle` →
expected: PASS. Then run the whole class: `./mvnw test -Dtest=WikiDesignConsistencyTest`.

- [ ] **Step 5: Commit**

`git commit -m "feat(bestiary): let every territory chip be revealed from the card"`

---

### Task 5: One styled dropdown for all four wiki pages

`custom-select` and `wiki-sort-select` have no CSS at all — that is why Bestiary, Prophecies and Equipment
render a raw white OS dropdown while Arcana only looks right because of an inline `style` attribute. One
`.wiki-select` class replaces all three, and the Equipment sort control moves inside its filter box.

**Files:**
- Modify: `src/main/resources/static/css/main.css` (add `.wiki-select`)
- Modify: `src/main/resources/templates/wiki/arcana.html` (drop the inline style, use `wiki-select`)
- Modify: `src/main/resources/templates/wiki/prophecies.html` (styled location dropdown)
- Modify: `src/main/resources/templates/wiki/bestiary.html` (Gnosis pill row out, grouped location dropdown
  + sort in, counter out)
- Modify: `src/main/resources/templates/wiki/index.html` (sort in)
- Modify: `src/main/resources/templates/wiki/fragments/item-grid.html` (sort out)
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: `getLocationGroups()` (Task 2)
- Produces: `.wiki-select` on every `<select>` in `src/main/resources/templates`; the Equipment page has
  `#sort-select` inside `.wiki-filter-box`, and the `/wiki/items` fragment no longer contains it

- [ ] **Step 1: Write the failing tests**

```java
    @Test
    void shouldStyleEveryWikiDropdownWithTheSharedSelectClass() throws Exception {
        String css = mainCss();
        String rule = cssRule(css, ".wiki-select");
        assertThat(rule).contains("appearance: none");
        assertThat(rule).contains("background-color: #201d19");

        for (String template : List.of("wiki/bestiary", "wiki/prophecies", "wiki/arcana", "wiki/index")) {
            assertThat(readTemplate(template + ".html"))
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
    void shouldKeepTheEquipmentSortControlInsideTheFilterBox() throws Exception {
        String page = mockMvc.perform(get("/wiki"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page.indexOf("wiki-filter-box")).isLessThan(page.indexOf("id=\"sort-select\""));
        assertThat(page.indexOf("id=\"sort-select\"")).isLessThan(page.indexOf("id=\"wiki-items-grid\""));

        // …and the swapped fragment must not carry it, or the swap would reset it.
        mockMvc.perform(get("/wiki/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sort-select"))));
    }
```

Update `shouldRenderDropdownForArcanaProphecyCategories` to assert `wiki-select` instead of
`custom-select`. Add a helper to the test class:

```java
    private static String readTemplate(String path) throws IOException {
        return Files.readString(Path.of("src/main/resources/templates", path));
    }
```

(`java.nio.file.Files`, `java.nio.file.Path`, `java.io.IOException`, `java.util.List` imports.)

- [ ] **Step 2: Run the tests — verify they FAIL**

`./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: the three new tests fail
(`.wiki-select` rule missing, `custom-select` still present, sort still in the fragment).

- [ ] **Step 3: Write minimal implementation**

Add to `main.css` after `.filter-secondary-label`:

```css
/* The one dropdown style for every wiki page (was an inline style on Arcana only). */
.wiki-select {
  -webkit-appearance: none;
  appearance: none;
  background-color: #201d19;
  background-image: url("data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 12 8'%3E%3Cpath d='M1 1.5 6 6.5l5-5' fill='none' stroke='%23ddaf7a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.7rem center;
  background-size: 0.7rem;
  color: #e5e7eb;
  border: 1px solid var(--wf-border);
  border-radius: 0.375rem;
  padding: 0.45rem 2rem 0.45rem 0.85rem;
  font-family: inherit;
  font-size: 0.85rem;
  cursor: pointer;
  min-width: 13rem;
  max-width: 22rem;
}

.wiki-select:hover {
  border-color: rgba(var(--wf-gold-rgb), 0.45);
}

.wiki-select:focus {
  outline: none;
  border-color: var(--wf-gold);
  box-shadow: 0 0 0 2px rgba(var(--wf-gold-rgb), 0.18);
}

.wiki-select optgroup {
  background: #17140f;
  color: var(--wf-gold);
  font-style: normal;
  font-weight: 700;
  font-size: 0.72rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.wiki-select option {
  background: #201d19;
  color: #e5e7eb;
}
```

`arcana.html`: `class="custom-select"` → `class="wiki-select"` and delete the whole `style="…"`
attribute on that `<select>`.

`prophecies.html`: give the location `<select>` `class="wiki-select"` (already `custom-select`) and keep
the existing `.filter-controls-row` layout.

`index.html`: insert a sort field into the filter box, before the Reset Filters block:

```html
            <!-- Sort -->
            <div class="filter-secondary-row">
                <div class="filter-secondary-field">
                    <label class="filter-secondary-label" for="sort-select">Sort:</label>
                    <select id="sort-select"
                            class="wiki-select"
                            x-model="sortCriteria"
                            @change="updateFilters()">
                        <option value="category">By Category</option>
                        <option value="name-asc">Name (A-Z)</option>
                        <option value="name-desc">Name (Z-A)</option>
                        <option value="element-asc">Element (A-Z)</option>
                        <option value="element-desc">Element (Z-A)</option>
                    </select>
                </div>

                <div x-show="selectedCategory || selectedElement || noElement || searchQuery">
                    <button type="button" class="filter-pill-btn filter-reset-btn" @click="clearAllFilters()">
                        Reset Filters
                    </button>
                </div>
            </div>
```

replacing the existing standalone Reset block (lines 123–131) so there is one level of separators, and
delete the sort markup from `item-grid.html` (lines 11–23), leaving its header as:

```html
    <div class="wiki-results-header">
        <div class="results-counter">
            Showing <span th:text="${items.size()}">0</span> <span th:text="${items.size() == 1 ? 'item' : 'items'}">items</span>
        </div>
    </div>
```

`bestiary.html`: delete the whole Gnosis block (lines 69–89), drop `selectedGnosis` from `x-data`,
`updateFilters()` and `clearAllFilters()`, replace the location `<select>` with the grouped one, and move
the sort select into the filter box next to it:

```html
            <div class="filter-secondary-row">
                <div class="filter-secondary-field">
                    <label class="filter-secondary-label" for="location-select">Location:</label>
                    <select id="location-select"
                            class="wiki-select"
                            x-model="selectedLocation"
                            @change="updateFilters()">
                        <option value="">All Locations</option>
                        <optgroup th:each="group : ${locationGroups}" th:label="${group.key}">
                            <option th:each="option : ${group.value}"
                                    th:value="${option.value}"
                                    th:text="${option.label}">Irongate Castle</option>
                        </optgroup>
                    </select>
                </div>

                <div class="filter-secondary-field">
                    <label class="filter-secondary-label" for="sort-select">Sort:</label>
                    <select id="sort-select"
                            class="wiki-select"
                            x-model="sortCriteria"
                            @change="updateFilters()">
                        <option value="name">Name (A-Z)</option>
                        <option value="health">Health (High to Low)</option>
                    </select>
                </div>

                <div x-show="searchQuery || selectedLocation || sortCriteria !== 'name'">
                    <button type="button" class="filter-pill-btn filter-reset-btn" @click="clearAllFilters()">
                        Reset Filters
                    </button>
                </div>
            </div>
```

Delete the page-level `.wiki-results-header` block from `bestiary.html` (the counter moves in Task 6), and
update the search placeholder to `Search enemies by name, location, or attack...` so it matches what the
repository now searches.

Also drop the last Gnosis mention, the empty-state copy in `enemy-grid.html` (line 7):
`Try adjusting your search criteria or Gnosis level.` → `Try adjusting your search or location filter.`

- [ ] **Step 4: Run the tests — verify they PASS**

`./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "feat(wiki): give every page one styled dropdown and move the equipment sort into the filter box"`

---

### Task 6: Result counters travel with the swapped fragment

*(Extra — fixes an existing bug surfaced by the new filter. Bestiary, Arcana and Prophecies render
“Showing N …” in the page while HTMX swaps only the grid, so the counter is stale after every filter
change. Skippable if you would rather keep this change set tight.)*

**Files:**
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Modify: `src/main/resources/templates/wiki/fragments/arcana-grid.html`
- Modify: `src/main/resources/templates/wiki/fragments/prophecy-list.html`
- Modify: `src/main/resources/templates/wiki/bestiary.html`, `wiki/arcana.html`, `wiki/prophecies.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: the `enemies` / `cards` / `prophecies` model attributes already passed to the fragment
  endpoints
- Produces: every swapped fragment root contains `.wiki-results-header > .results-counter`, and no page
  renders a counter outside its grid

- [ ] **Step 1: Write the failing test**

```java
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

        // The counter must live inside the swapped container, not beside it.
        String page = mockMvc.perform(get("/wiki/bestiary"))
                .andReturn().getResponse().getContentAsString();
        assertThat(page.indexOf("results-counter"))
                .isGreaterThan(page.indexOf("id=\"bestiary-enemies-grid\""));
    }
```

- [ ] **Step 2: Run the test — verify it FAILS**

`./mvnw test -Dtest=WikiDesignConsistencyTest#shouldRefreshTheResultCounterWithTheSwappedFragment` →
expected: FAIL on the first assertion (`/wiki/bestiary/enemies` returns only the grid).

- [ ] **Step 3: Write minimal implementation**

Wrap each fragment: outer element keeps the id, gains nothing else; the header sits above an inner grid.

`enemy-grid.html`:

```html
<div th:fragment="enemyGrid" id="bestiary-enemies-grid">
    <div class="wiki-results-header">
        <div class="results-counter">
            Showing <span th:text="${#lists.size(enemies)}">0</span>
            <span th:text="${#lists.size(enemies) == 1 ? 'enemy' : 'enemies'}">enemies</span>
        </div>
    </div>

    <div class="bestiary-grid">
        <!-- existing empty state + article th:each, unchanged -->
    </div>
</div>
```

`arcana-grid.html` and `prophecy-list.html`: same shape, using `${#lists.size(cards)} Arcana cards` and
`${#lists.size(prophecies)} prophecies`, with `.arcana-grid` / `.prophecy-grid` as the inner grid.

Delete the now-duplicated `.wiki-results-header` blocks from `bestiary.html`, `arcana.html` and
`prophecies.html`.

- [ ] **Step 4: Run the test — verify it PASSES**

`./mvnw test -Dtest=WikiDesignConsistencyTest#shouldRefreshTheResultCounterWithTheSwappedFragment` →
expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "fix(wiki): refresh result counters with the htmx-swapped fragment"`

---

### Task 7: Controller test alignment and full verification

**Files:**
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiControllerTest.java`

- [ ] **Step 1: Update the controller tests**

`shouldRenderBestiaryPage`: `model().attributeExists("enemies", "locations", "locationGroups")` — replace
`"gnosisLevels"` with `"locationGroups"` and add
`.andExpect(model().attributeDoesNotExist("gnosisLevels"))`.

`shouldReturnEnemyGridFragment`: replace `.param("gnosis", "3")` with
`.param("location", "Irongate Castle")` and keep the `Anointer` assertion (Anointer is in Irongate Castle).

- [ ] **Step 2: Run the class — verify it PASSES**

`./mvnw test -Dtest=BestiaryWikiControllerTest` → expected: 4 tests, 0 failures.

- [ ] **Step 3: Full suite**

`./mvnw test` → expected: **all tests pass, BUILD SUCCESS** (was 110 tests; expect ~120).

- [ ] **Step 4: Visual verification on the running app**

Restart the app (Java changed): `./mvnw -o spring-boot:run -Dspring-boot.run.arguments=--server.port=9091`
as a background job, then shoot with the cold-profile Chromium on port 9340:

- `/wiki/bestiary` full page — rank badge top-right, no Gnosis anywhere, grouped dropdown styled, sort in
  the box
- Axeman card — `FORMS  ELITE  ASCENDED`
- Blunderbusser card — 3 chips + `+4 more`
- `/wiki/prophecies`, `/wiki`, `/wiki/arcana` — every dropdown dark, Equipment sort inside the filter box
- Probe `getComputedStyle(select).appearance` and `backgroundColor` to prove the styling applied (a cold
  `--user-data-dir` is mandatory; Chrome caches CSS heuristically)

- [ ] **Step 5: Commit**

`git commit -m "test(bestiary): align controller tests with the location-first filter"`

---

## Self-review

- **Spec coverage:** item 1 → Tasks 2 + 5 (filter replaced, badge removed in Task 3). item 2 → Task 3.
  item 3 → Task 4. Bestiary/Prophecies/Equipment dropdowns → Task 5. Equipment sort → Task 5. No spec item
  without a task.
- **Placeholders:** none — every step carries the test or the code.
- **Type consistency:** `search(query, rank, location, sort)` is used with that arity in Task 2's tests, the
  controller update and Task 7; `getVariantForms()` (Task 1) is consumed once, in Task 3;
  `getLocationGroups()` (Task 2) is consumed once, in Task 5; `LocationOption` is a nested record, so tests
  reference it as `EnemyRepository.LocationOption`.
- **Not touched on purpose:** the `gnosis` data field in `enemies.json` / `Enemy.java` (scraped truth — a
  re-scrape would only re-add it), the unused `rank` search parameter, and the unreachable
  `enemy-modal.html` beyond removing its Gnosis badge.
