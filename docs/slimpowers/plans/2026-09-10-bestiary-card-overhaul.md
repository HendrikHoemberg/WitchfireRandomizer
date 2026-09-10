# Bestiary Catalog Overhaul Implementation Plan

**Goal:** Rebuild the Bestiary catalog so it carries the same visual weight, card anatomy, and
information density as the Equipment, Arcana, and Prophecies pages.

**Architecture:** Server-rendered Thymeleaf only — no controller, repository, model, or data
changes. `enemy-grid.html` is restructured from a loose badge/chip soup into the same three-band
card anatomy the other catalog pages use (header → labeled stat block → labeled data block →
footer), with enemy affinities grouped into explicit `Resists` / `Vulnerable` rows and an explicit
empty state. `main.css` gets the harmonized grid track plus the new card classes, and the now-dead
card rules are removed.

**Tech Stack:** Spring Boot 4.1 / Java 25, Thymeleaf, HTMX + Alpine.js, hand-written `main.css`
(no Node build step). Tests: JUnit 5 + `@SpringBootTest`/MockMvc.

**Spec:** No spec doc — driven by the four wiki page screenshots in
`target/ui-review/shots-current/` (`01-wiki-equipment.png`, `02-wiki-arcana.png`,
`03-wiki-prophecies.png`, `04-wiki-bestiary.png`).

## Global Constraints

- Verify command: `./mvnw test`; single test: `./mvnw test -Dtest=WikiDesignConsistencyTest#methodName`.
- One gold only: `--wf-gold` / `--wf-gold-rgb` / `--wf-gold-accent` / `--wf-gold-hover` / `--wf-gold-bright`. Never hardcode another gold.
- Every CSS variable used must be defined in `:root` (`shouldDefineEveryCssVariableItUses`).
- Card surfaces come from the shared `.texture-box` glass; do not add an opaque wiki-only card background.
- No saturated neon colors (`#34d399`, `16, 185, 129`, `#c084fc` are forbidden).
- Do not touch: `.mvn/wrapper/*`, `target/*`, `src/main/resources/data/*.json`, `enemy-modal.html`.
- Out of scope (recorded, not changed): `enemy-modal.html` is unreachable from any page — no
  template includes it and the bestiary cards no longer open a modal. It stays for now.

---
```

---
## The problem (from the screenshots)

| Page | Card anatomy | Grid |
|---|---|---|
| Equipment | icon frame + 1.5rem title + element pill → 4-col labeled stat grid → Description section → Mysterium levels | 1 column, full band |
| Arcana | 3.25rem icon + 1.15rem title + element + tag row → description paragraph → gold-labeled `EFFECT:` box | `repeat(auto-fill, minmax(340px, 1fr))` |
| Prophecies | 3.5rem icon + title + `arcana-tag` + location → description → red `IMPOSED OMEN` box → footer action | `repeat(auto-fill, minmax(320px, 1fr))` |
| **Bestiary** | 3.5rem portrait + title + floating `GNOSIS n` + loose `Minor`/`ELITE` chips → unlabeled HP/ATK chips → unlabeled 0.72rem resistance pills → 1–3 wrapping rows of location chips | hardcoded `repeat(2, 1fr)` |

**Decision (confirmed with the user): the page keeps its 2-column grid.** The other catalogs are
3-up because their cards are narrow; the Bestiary's wider cards stay. Harmonization comes from the
card anatomy, typography, and labeled bands — not from the column count.

Concrete defects this plan fixes:

1. **No card anatomy** — no labeled data block, no band separation; the card reads as metadata soup.
2. **Ragged rows** — `align-items: start` + 1–3 location chip rows give every card a different height.
3. **Weak hierarchy** — the gameplay-critical resistance data is the smallest, least salient text on the card.
4. **Dead header space** — a ~700px-wide header holds a 56px portrait and a title while the gnosis seal floats at the far edge, so the card's weight is not where the eye lands; the wide row is only filled by wrapping chip soup.
5. **Empty-looking cards** — 18 of 62 enemies have zero elemental affinities, so the strip is simply absent and the card looks broken.
6. **Duplicated rank** — 10 enemies have `rank == name` (`Sepulcher`, `Warden`, `Stone Golem`, …), rendering the name twice.

Data facts that drive the design (verified against `src/main/resources/data/enemies.json`):

- `description` is empty for **all 62** enemies → the card cannot rely on a lore line.
- Affinities: `staggerResistance` on 39, `air` 31, `freeze` 27, `fire` 17, `burn` 10, `decay` 10,
  `earth` 7, `water` 7, `shock` 2, `stun` 0. **Only 7 enemies have any vulnerability, all to Decay.**
  → There is no point adding a "filter by vulnerability" feature; it would return nothing for 7 of 8
  elements. This is why the plan is purely presentational.
- Locations per enemy: 1–7 (mode 1). → clamp to 3 + overflow indicator.
- `variants`: `""` (26), `Elite` (14), `Elite / Ascended` (22). `gnosis`: 0–5.

---
## Target card anatomy

```
┌──────────────────────────────────────────────────────────┐
│ ┌──────┐  Anointer                        [ GNOSIS 3 ]   │
│ │portra│  ┌ Faithful ┐ ┌ ELITE / ASCENDED ┐              │
│ └──────┘                                                 │
│  ┌──────────────────────┬──────────────────────┐         │
│  │ HEALTH               │ ATTACK               │         │  ← .enemy-stat-grid
│  │ 2,600                │ 4 - 35               │         │
│  └──────────────────────┴──────────────────────┘         │
│  RESISTS        [Fire +75%] [Freeze +50%] [Air +35%]     │  ← .enemy-affinity-row
│                 [Stagger 80%]                            │
│  VULNERABLE     [Decay -100%]                            │  ← .enemy-affinity-row.is-vulnerable
│  ──────────────────────────────────────────────────────  │
│  TERRITORY      [Irongate Castle]                        │  ← footer, max 3 + "+N more"
└──────────────────────────────────────────────────────────┘
```

Empty-affinity cards render `No elemental resistances or weaknesses.` in the affinity block, so
every card keeps the same bands and the row stays even. The wide 2-up card is filled by the stat
block and the affinity pills, which run the full card width; `align-items: stretch` plus the
3-territory clamp keeps every card in a row the same height.

---
## Task 1: Regression tests for the harmonized bestiary catalog

**Files:**
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: `GET /wiki/bestiary` and `GET /wiki/bestiary/enemies` (existing, `search` param filters
  by name), `src/main/resources/static/css/main.css` (read as text).
- Produces: the failing spec that Task 2 must satisfy — `.bestiary-grid` track sizing, `.enemy-stat-grid`
  / `.enemy-stat-label` stat block, `.enemy-affinity-label` with `Resists` + `Vulnerable`, the
  no-affinity empty state, the `+N more` territory overflow chip, and no duplicate rank tag.

- [ ] **Step 1: Keep the existing two-column test and add the new failing tests**

Keep the existing test `shouldStyleBestiaryInTwoColumnGridAndOmitPlaceholderText` (lines 206–218)
**unchanged** — it already pins the confirmed 2-column grid and the placeholder omission, and it
must stay green throughout. Add these five tests after it:

```java
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
    void shouldGroupEnemyAffinitiesIntoResistsAndVulnerabilities() throws Exception {
        // Anointer resists Fire/Freeze/Air/Stagger and is vulnerable to Decay.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enemy-affinity-label")))
                .andExpect(content().string(containsString(">Resists<")))
                .andExpect(content().string(containsString(">Vulnerable<")))
                .andExpect(content().string(containsString("enemy-affinity-row is-vulnerable")))
                .andExpect(content().string(not(containsString("enemy-affinity-strip"))));
    }

    @Test
    void shouldRenderAnExplicitEmptyStateForEnemiesWithoutAffinities() throws Exception {
        // Assassin has no elemental resistances and no vulnerabilities.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Assassin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No elemental resistances or weaknesses.")));
    }

    @Test
    void shouldCondenseEnemyTerritoriesWithAnOverflowChip() throws Exception {
        // Arcabusier lists 4 territories; the card shows 3 plus an overflow indicator.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Arcabusier"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("location-chip-more")))
                .andExpect(content().string(containsString("+1 more")));

        // Anointer lists 1 territory; no overflow chip.
        mockMvc.perform(get("/wiki/bestiary/enemies").param("search", "Anointer"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("location-chip-more"))));
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
```

- [ ] **Step 2: Run the tests — verify they FAIL**

```bash
./mvnw test -Dtest=WikiDesignConsistencyTest
```

Expected: 5 failures (the rest of the class passes, including
`shouldStyleBestiaryInTwoColumnGridAndOmitPlaceholderText`). Failure reasons, not compile errors:
`shouldRenderALabeledCombatStatBlockOnEnemyCards` → missing `enemy-stat-grid`;
`shouldGroupEnemyAffinitiesIntoResistsAndVulnerabilities` → missing `enemy-affinity-label`;
`shouldRenderAnExplicitEmptyStateForEnemiesWithoutAffinities` → missing the empty-state sentence;
`shouldCondenseEnemyTerritoriesWithAnOverflowChip` → missing `location-chip-more`;
`shouldNotDuplicateTheRankTagOnUniqueEnemies` → Sepulcher still renders `enemy-rank-tag`.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java
git commit -m "test(bestiary): specify the harmonized bestiary card anatomy"
```

---
## Task 2: Rebuild the enemy card fragment and CSS

**Files:**
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html` — full rewrite of the card body (keep the fragment id, the `th:each`, and the empty state).
- Modify: `src/main/resources/static/css/main.css` — `.bestiary-grid` track, new card classes, delete dead card rules.

**Interfaces:**
- Consumes: `enemies` (model attribute, `List<Enemy>`), `Enemy` getters `name`, `rank`, `gnosis`,
  `health`, `damage`, `variants`, `locations`, and the ten `*Resistance` getters.
- Produces: the rendered classes the Task 1 tests assert — `enemy-stat-grid`, `enemy-stat-label`,
  `enemy-affinity-label`, `enemy-affinity-row is-vulnerable`, `enemy-affinity-none`,
  `enemy-rank-tag`, `enemy-variant-tag`, `location-chip-more`.

- [ ] **Step 1: Replace the card markup in `enemy-grid.html`**

Replace the whole `<div th:fragment="enemyGrid" …>` … `</div>` block (lines 4–98) with:

```html
<div th:fragment="enemyGrid" id="bestiary-enemies-grid" class="bestiary-grid">
    <div th:if="${#lists.isEmpty(enemies)}" class="empty-state texture-box" style="grid-column: 1 / -1; padding: 3rem; text-align: center;">
        <h3 style="color: var(--wf-gold); font-size: 1.25rem; margin-bottom: 0.5rem;">No Enemies Found</h3>
        <p style="color: #9ca3af; font-size: 0.95rem;">Try adjusting your search criteria or Gnosis level.</p>
    </div>

    <article th:each="enemy : ${enemies}"
             class="enemy-card texture-box"
             th:id="${enemy.id}"
             th:with="hasResistance=${(enemy.fireResistance != null and enemy.fireResistance > 0)
                     or (enemy.shockResistance != null and enemy.shockResistance > 0)
                     or (enemy.freezeResistance != null and enemy.freezeResistance > 0)
                     or (enemy.decayResistance != null and enemy.decayResistance > 0)
                     or (enemy.airResistance != null and enemy.airResistance > 0)
                     or (enemy.earthResistance != null and enemy.earthResistance > 0)
                     or (enemy.waterResistance != null and enemy.waterResistance > 0)
                     or (enemy.burnResistance != null and enemy.burnResistance > 0)
                     or (enemy.staggerResistance != null and enemy.staggerResistance > 0)
                     or (enemy.stunResistance != null and enemy.stunResistance > 0)},
                     hasVulnerability=${(enemy.fireResistance != null and enemy.fireResistance < 0)
                     or (enemy.shockResistance != null and enemy.shockResistance < 0)
                     or (enemy.freezeResistance != null and enemy.freezeResistance < 0)
                     or (enemy.decayResistance != null and enemy.decayResistance < 0)
                     or (enemy.airResistance != null and enemy.airResistance < 0)
                     or (enemy.earthResistance != null and enemy.earthResistance < 0)
                     or (enemy.waterResistance != null and enemy.waterResistance < 0)
                     or (enemy.burnResistance != null and enemy.burnResistance < 0)}">
        <!-- Header: portrait, name, rank/variant tags, gnosis seal -->
        <header class="enemy-card-header">
            <div class="enemy-portrait-box">
                <img th:if="${enemy.iconUrl != null}" th:src="${enemy.iconUrl}" th:alt="${enemy.name}" loading="lazy" />
            </div>
            <div class="enemy-card-heading">
                <div class="enemy-card-title-row">
                    <h3 class="enemy-card-title" th:text="${enemy.name}">Enemy Name</h3>
                    <span th:if="${enemy.gnosis != null}"
                          class="gnosis-badge"
                          th:text="'Gnosis ' + ${enemy.gnosis}">Gnosis 0</span>
                </div>
                <div class="enemy-tags-row">
                    <span th:if="${enemy.rank != null and !enemy.rank.isBlank() and !enemy.rank.equalsIgnoreCase(enemy.name)}"
                          class="arcana-tag enemy-rank-tag"
                          th:text="${enemy.rank}">Rank</span>
                    <span th:if="${enemy.variants != null and !enemy.variants.isBlank()}"
                          class="arcana-tag enemy-variant-tag"
                          th:text="${enemy.variants}">Elite</span>
                </div>
            </div>
        </header>

        <!-- Body: labeled combat stat block, grouped affinities, territory footer -->
        <div class="enemy-card-body">
            <div class="enemy-stat-grid">
                <div class="enemy-stat">
                    <span class="enemy-stat-label">Health</span>
                    <span class="enemy-stat-value"
                          th:text="${enemy.health != null and enemy.health > 0 ? #numbers.formatInteger(enemy.health, 1, 'COMMA') : '—'}">2,600</span>
                </div>
                <div class="enemy-stat">
                    <span class="enemy-stat-label">Attack</span>
                    <span class="enemy-stat-value"
                          th:text="${enemy.damage != null and !enemy.damage.isBlank() ? enemy.damage : '—'}">4 - 35</span>
                </div>
            </div>

            <div class="enemy-affinity-block">
                <div class="enemy-affinity-row" th:if="${hasResistance}">
                    <span class="enemy-affinity-label">Resists</span>
                    <div class="enemy-affinity-pills">
                        <span th:if="${enemy.fireResistance != null and enemy.fireResistance > 0}" class="affinity-pill aff-resistant" th:text="'Fire +' + ${enemy.fireResistance} + '%'">Fire +75%</span>
                        <span th:if="${enemy.shockResistance != null and enemy.shockResistance > 0}" class="affinity-pill aff-resistant" th:text="'Shock +' + ${enemy.shockResistance} + '%'">Shock +35%</span>
                        <span th:if="${enemy.freezeResistance != null and enemy.freezeResistance > 0}" class="affinity-pill aff-resistant" th:text="'Freeze +' + ${enemy.freezeResistance} + '%'">Freeze +50%</span>
                        <span th:if="${enemy.decayResistance != null and enemy.decayResistance > 0}" class="affinity-pill aff-resistant" th:text="'Decay +' + ${enemy.decayResistance} + '%'">Decay +100%</span>
                        <span th:if="${enemy.airResistance != null and enemy.airResistance > 0}" class="affinity-pill aff-resistant" th:text="'Air +' + ${enemy.airResistance} + '%'">Air +35%</span>
                        <span th:if="${enemy.earthResistance != null and enemy.earthResistance > 0}" class="affinity-pill aff-resistant" th:text="'Earth +' + ${enemy.earthResistance} + '%'">Earth +20%</span>
                        <span th:if="${enemy.waterResistance != null and enemy.waterResistance > 0}" class="affinity-pill aff-resistant" th:text="'Water +' + ${enemy.waterResistance} + '%'">Water +20%</span>
                        <span th:if="${enemy.burnResistance != null and enemy.burnResistance > 0}" class="affinity-pill aff-resistant" th:text="'Burn +' + ${enemy.burnResistance} + '%'">Burn +20%</span>
                        <span th:if="${enemy.staggerResistance != null and enemy.staggerResistance > 0}" class="affinity-pill aff-physical" th:text="'Stagger ' + ${enemy.staggerResistance} + '%'">Stagger 80%</span>
                        <span th:if="${enemy.stunResistance != null and enemy.stunResistance > 0}" class="affinity-pill aff-physical" th:text="'Stun ' + ${enemy.stunResistance} + '%'">Stun 50%</span>
                    </div>
                </div>

                <div class="enemy-affinity-row is-vulnerable" th:if="${hasVulnerability}">
                    <span class="enemy-affinity-label">Vulnerable</span>
                    <div class="enemy-affinity-pills">
                        <span th:if="${enemy.fireResistance != null and enemy.fireResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Fire ' + ${enemy.fireResistance} + '%'">Fire -50%</span>
                        <span th:if="${enemy.shockResistance != null and enemy.shockResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Shock ' + ${enemy.shockResistance} + '%'">Shock -50%</span>
                        <span th:if="${enemy.freezeResistance != null and enemy.freezeResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Freeze ' + ${enemy.freezeResistance} + '%'">Freeze -50%</span>
                        <span th:if="${enemy.decayResistance != null and enemy.decayResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Decay ' + ${enemy.decayResistance} + '%'">Decay -100%</span>
                        <span th:if="${enemy.airResistance != null and enemy.airResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Air ' + ${enemy.airResistance} + '%'">Air -50%</span>
                        <span th:if="${enemy.earthResistance != null and enemy.earthResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Earth ' + ${enemy.earthResistance} + '%'">Earth -50%</span>
                        <span th:if="${enemy.waterResistance != null and enemy.waterResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Water ' + ${enemy.waterResistance} + '%'">Water -50%</span>
                        <span th:if="${enemy.burnResistance != null and enemy.burnResistance < 0}" class="affinity-pill aff-vulnerable" th:text="'Burn ' + ${enemy.burnResistance} + '%'">Burn -50%</span>
                    </div>
                </div>

                <p th:if="${!hasResistance and !hasVulnerability}" class="enemy-affinity-none">
                    No elemental resistances or weaknesses.
                </p>
            </div>

            <div th:if="${enemy.locations != null and !enemy.locations.isEmpty()}" class="enemy-locations-row">
                <span class="enemy-locations-label">Territory</span>
                <div class="locations-chips-row">
                    <span th:each="loc, iter : ${enemy.locations}"
                          th:if="${iter.index < 3}"
                          class="location-chip"
                          th:text="${loc}">Irongate Castle</span>
                    <span th:if="${enemy.locations.size() > 3}"
                          class="location-chip location-chip-more"
                          th:text="|+${enemy.locations.size() - 3} more|">+2 more</span>
                </div>
            </div>
        </div>
    </article>
</div>
```

- [ ] **Step 2: Rewrite the bestiary CSS block in `main.css`**

Replace the block from `.bestiary-grid {` (line 1476) through the end of `.enemy-desc { … }`
(line 1612) with the following. `.affinity-pill`, `.aff-resistant`, `.aff-vulnerable`,
`.aff-physical`, `.gnosis-badge` keep their existing definitions — they are reused unchanged.

```css
.bestiary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
  margin-top: 1.5rem;
  align-items: stretch;
}

@media (max-width: 900px) {
  .bestiary-grid {
    grid-template-columns: 1fr;
  }
}

.enemy-card {
  border: 1px solid var(--wf-border);
  border-radius: 0.5rem;
  padding: 1.15rem;
  display: flex;
  flex-direction: column;
  transition: transform 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.enemy-card:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--wf-gold-rgb), 0.4);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
}

.enemy-card-header {
  display: flex;
  gap: 0.85rem;
  align-items: flex-start;
  margin-bottom: 0.85rem;
}

.enemy-portrait-box {
  width: 3.5rem;
  height: 3.5rem;
  background: #000;
  border: 1px solid var(--wf-border);
  border-radius: 0.375rem;
  padding: 0.2rem;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.enemy-portrait-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0.25rem;
}

.enemy-card-heading {
  flex-grow: 1;
  min-width: 0;
}

.enemy-card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}

.enemy-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.enemy-rank-tag {
  color: #d1d5db;
}

.enemy-variant-tag {
  color: var(--wf-gold);
  background: rgba(var(--wf-gold-rgb), 0.12);
  border-color: rgba(var(--wf-gold-rgb), 0.35);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
}

.enemy-card-body {
  display: flex;
  flex-direction: column;
}

.enemy-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.4rem 1rem;
  padding: 0.6rem 0.75rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid var(--wf-border-subtle);
  border-radius: 0.35rem;
  margin-bottom: 0.85rem;
}

.enemy-stat {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.enemy-stat-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--wf-gold);
}

.enemy-stat-value {
  font-size: 0.95rem;
  font-weight: 600;
  color: #f3f4f6;
}

.enemy-affinity-block {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.enemy-affinity-row {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
}

.enemy-affinity-label {
  flex: 0 0 4.75rem;
  padding-top: 0.2rem;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #94a3b8;
}

.enemy-affinity-row.is-vulnerable .enemy-affinity-label {
  color: #f87171;
}

.enemy-affinity-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.enemy-affinity-none {
  font-size: 0.8rem;
  font-style: italic;
  color: #9ca3af;
}

.enemy-locations-row {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px solid var(--wf-border-subtle);
}

.enemy-locations-label {
  flex: 0 0 4.75rem;
  padding-top: 0.2rem;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--wf-gold);
}

.location-chip-more {
  color: var(--wf-gold);
  border-color: rgba(var(--wf-gold-rgb), 0.35);
}
```

Then delete the now-unused rules, which the fragment no longer emits and no other template uses:
`.enemy-combat-stats-row`, `.enemy-affinity-strip`, `.enemy-desc`, and `.rank-badge` (the last one
is still referenced by `enemy-modal.html` — **keep `.rank-badge`**, only delete the first three).

- [ ] **Step 3: Run the tests — verify they PASS**

```bash
./mvnw test -Dtest=WikiDesignConsistencyTest
```

Expected: PASS, 0 failures.

- [ ] **Step 4: Run the full suite**

```bash
./mvnw test
```

Expected: BUILD SUCCESS, 0 failures / 0 errors (the `BestiaryWikiControllerTest` and
`WikiNavigationTest` suites cover the routes and must stay green).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/wiki/fragments/enemy-grid.html src/main/resources/static/css/main.css
git commit -m "style(bestiary): rebuild enemy cards with the shared catalog card anatomy"
```

---
## Task 3: Page-level harmonization, verification, and docs

**Files:**
- Modify: `src/main/resources/templates/wiki/bestiary.html` — replace the inline-styled secondary filter row with the shared `.filter-secondary-row` class and a `filter-section-title` heading, matching Arcana/Prophecies.
- Modify: `src/main/resources/static/css/main.css` — add `.filter-secondary-row`.
- Modify: `docs/slimpowers/plans/current-task.md` — append the completed checklist.

**Interfaces:**
- Consumes: Task 2's fragment and CSS.
- Produces: a bestiary filter box whose Location/Reset row uses the same spacing and divider idiom as the rest of the wiki; a verified, screenshot-checked page.

- [ ] **Step 1: Add `.filter-secondary-row` to `main.css`**

Add immediately after the `.filter-pills-row` rule (line 1047):

```css
.filter-secondary-row {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--wf-border);
}

.filter-secondary-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-secondary-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--wf-text-muted);
}
```

- [ ] **Step 2: Replace the inline-styled row in `bestiary.html`**

Replace lines 92–113 (the secondary filters block) with:

```html
            <div class="filter-secondary-row">
                <div class="filter-secondary-field">
                    <label class="filter-secondary-label">Location:</label>
                    <select class="custom-select"
                            x-model="selectedLocation"
                            @change="updateFilters()">
                        <option value="">All Locations</option>
                        <option th:each="loc : ${locations}" th:value="${loc}" th:text="${loc}">Location</option>
                    </select>
                </div>

                <div x-show="searchQuery || selectedGnosis !== '' || selectedLocation || sortCriteria !== 'name'">
                    <button type="button"
                            class="filter-pill-btn filter-reset-btn"
                            @click="clearAllFilters()">
                        Reset Filters
                    </button>
                </div>
            </div>
```

- [ ] **Step 3: Run the full suite**

```bash
./mvnw test
```

Expected: BUILD SUCCESS, 0 failures / 0 errors.

- [ ] **Step 4: Re-capture screenshots and inspect them**

```bash
./mvnw -o -q process-resources   # copy templates/CSS into target/classes for the running app
node target/ui-review/shoot-current.mjs 9333 http://127.0.0.1:9090 \
     target/ui-review/shots-current target/ui-review/spec-current.json
```

Then read `target/ui-review/shots-current/04-wiki-bestiary.png`,
`05-wiki-bestiary-full.png`, and `06-wiki-bestiary-mobile.png` alongside
`02-wiki-arcana.png` and `03-wiki-prophecies.png` and confirm: three equal-weight columns, even
card heights per row, labeled stat blocks, grouped `Resists`/`Vulnerable` rows with an explicit
empty state, a single-line `Territory` footer with `+N more`, and no duplicate rank tags.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/wiki/bestiary.html src/main/resources/static/css/main.css docs/slimpowers/plans/current-task.md
git commit -m "style(bestiary): harmonize the filter row with the other wiki catalogs"
```

---
## Self-Review

**Spec coverage:** defect 1 → `.enemy-stat-grid` + `.enemy-affinity-*` (Task 2); defect 2 → `align-items: stretch` + 3-chip clamp (Task 2); defect 3 → `.enemy-stat-label` / `.enemy-affinity-label` gold and red labels at 0.68rem uppercase vs 0.72rem flat pills (Task 2); defect 4 → labeled bands + full-width affinity pill rows filling the wide card (Task 2); defect 5 → `enemy-affinity-none` (Task 2); defect 6 → `!enemy.rank.equalsIgnoreCase(enemy.name)` (Task 2). Page-level filter idiom → Task 3. The 2-column grid is deliberately unchanged.

**Placeholder scan:** no TBD/TODO/"similar to"; every step carries the literal code or command.

**Type consistency:** class names asserted in Task 1 (`enemy-stat-grid`, `enemy-stat-label`,
`enemy-affinity-label`, `enemy-affinity-row is-vulnerable`, `enemy-affinity-none`,
`enemy-rank-tag`, `location-chip-more`) are exactly the classes emitted in Task 2 and styled in
Task 2 Step 2. `enemy-locations-label` / `enemy-card-heading` / `enemy-tags-row` are emitted and
styled but not asserted — that is intentional, the tests pin behaviour, not every class.
