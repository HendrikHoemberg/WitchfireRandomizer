# Wiki Layout Harmonization & Bestiary Redesign Implementation Plan

**Goal:** Transform the Witchfire Wiki across all sections into a visually cohesive, AAA dark-fantasy compendium with 4 direct subnav tabs, responsive 2-column Equipment, self-contained 3-column Arcana, and a 2-column "stats at a glance" Bestiary.

**Architecture:**
- **Navigation & Routing:** Elevate wiki sub-navigation from 3 tabs into 4 direct tabs (`Equipment`, `Arcana`, `Prophecies`, `Bestiary`) styled as an integrated dark-glass HUD segmented control. Add dedicated `/wiki/prophecies` controller mapping.
- **Layout & Grid Consistency:**
  - Equipment: Modern 2-column responsive grid on desktop with dark-glass Mysterium accordions (no muddy mustard fill).
  - Arcana: 3-column responsive grid with self-contained full descriptions and manifestation scaling (no hollow modal).
  - Prophecies: 3-column responsive grid with dedicated search and dark-glass omen styling.
  - Bestiary: 2-column desktop / 1-column mobile grid with prominent "stats at a quick glance" combat strip (HP, Attack, key elemental vulnerabilities/resistances), omission of scraped filler text, and clear dossier modal affordance.
- **Shared Toolbar Standards:** Uniform results counter, standardized search with vertically centered clear icon, and consistent "Sort By" and "Reset Filters" across all tabs.

**Tech Stack:** Spring Boot 4 / Java 26, Thymeleaf, HTMX, Alpine.js, CSS, JUnit 5 + MockMvc.

**Spec:** User feedback approving 4 direct tabs (1.A), self-contained Arcana cards (2.A), 2-column Equipment (3.A), and 2-column desktop / 1-column mobile Bestiary with quick-glance combat stats.

## Global Constraints
- Do not edit `.mvn/wrapper/*` or generated build artifacts.
- Verify (everything): `./mvnw test`; single test: `./mvnw test -Dtest=TestClassName#testMethodName`.
- No new external CSS or JS frameworks; leverage existing CSS variables (`--wf-gold`, `--wf-border`, `--wf-modal-bg`).
- Preserve all existing Thymeleaf fragments and HTMX partial endpoints for filtering.
- Pass existing and updated `WikiDesignConsistencyTest` assertions.

---

### Task 1: Navigation & Controller Routing Expansion (4 Direct Tabs)

**Files:**
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/ArcanaWikiController.java`
- Modify: `src/main/resources/templates/wiki/fragments/wiki-nav.html`
- Modify: `src/main/resources/templates/fragments/navigation.html`
- Modify: `src/main/resources/templates/wiki/arcana.html`
- Create: `src/main/resources/templates/wiki/prophecies.html`
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/ArcanaWikiControllerTest.java`
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiNavigationTest.java`
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Route: `GET /wiki/prophecies` -> renders `wiki/prophecies`
- Nav fragment: `wikiNav(activeSection)` with values `'equipment'`, `'arcana'`, `'prophecies'`, `'bestiary'`.

- [ ] **Step 1: Write the failing tests**
  Update `WikiNavigationTest` and `ArcanaWikiControllerTest` to assert that `GET /wiki/prophecies` returns status 200, marks `'prophecies'` active, and renders all 4 wiki tabs in the subnav.
- [ ] **Step 2: Run the test — verify it FAILS**
  `./mvnw test -Dtest=WikiNavigationTest`
- [ ] **Step 3: Implement routing and navigation templates**
  - Add `@GetMapping("/wiki/prophecies")` in `ArcanaWikiController`.
  - Update `wiki-nav.html` to include the 4 tabs: `Equipment` (`/wiki`), `Arcana` (`/wiki/arcana`), `Prophecies` (`/wiki/prophecies`), `Bestiary` (`/wiki/bestiary`).
  - Style `.wiki-subnav-tabs` as an integrated dark HUD segmented bar (`background: rgba(20, 18, 16, 0.7); border: 1px solid var(--wf-border); border-radius: 0.5rem;`).
  - Create dedicated `wiki/prophecies.html` template.
  - Simplify `wiki/arcana.html` by removing the inner mode toggle (`viewMode: 'cards' | 'prophecies'`).
- [ ] **Step 4: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiNavigationTest+ArcanaWikiControllerTest`
- [ ] **Step 5: Commit**
  `git commit -m "feat(wiki): elevate subnav to 4 direct tabs and add dedicated prophecies page"`

---

### Task 2: Equipment 2-Column Grid & Mysterium Polish

**Files:**
- Modify: `src/main/resources/templates/wiki/index.html`
- Modify: `src/main/resources/templates/wiki/fragments/item-grid.html`
- Modify: `src/main/resources/templates/wiki/fragments/item-card.html`
- Modify: `src/main/resources/static/css/main.css`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- CSS: `.wiki-cards-list` changed to responsive 2-column grid (`display: grid; grid-template-columns: repeat(auto-fit, minmax(480px, 1fr)); gap: 1.25rem;`). On mobile (`max-width: 768px`): 1 column.
- CSS: `.wiki-mysterium-level-header` updated to dark glass styling (`background: rgba(0, 0, 0, 0.4); border: 1px solid var(--wf-border-subtle); color: #e5e7eb;`), hovering with `--wf-gold-accent` border.
- Template: Search input clear button centered with `top: 50%; transform: translateY(-50%);`.

- [ ] **Step 1: Write the failing tests**
  Add assertions in `WikiDesignConsistencyTest` for `.wiki-cards-list` grid display and dark-glass Mysterium styling.
- [ ] **Step 2: Run the test — verify it FAILS**
  `./mvnw test -Dtest=WikiDesignConsistencyTest`
- [ ] **Step 3: Implement 2-column grid and Mysterium accordion polish**
  - Update `main.css` `.wiki-cards-list` and `.wiki-mysterium-level-header`.
  - Fix search input clear button in `index.html`.
- [ ] **Step 4: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiDesignConsistencyTest`
- [ ] **Step 5: Commit**
  `git commit -m "style(equipment): convert to responsive 2-column grid and polish mysterium headers"`

---

### Task 3: Self-Contained 3-Column Arcana & Refined Prophecies

**Files:**
- Modify: `src/main/resources/templates/wiki/arcana.html`
- Modify: `src/main/resources/templates/wiki/fragments/arcana-grid.html`
- Modify: `src/main/resources/templates/wiki/prophecies.html`
- Modify: `src/main/resources/templates/wiki/fragments/prophecy-list.html`
- Modify: `src/main/resources/static/css/main.css`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- `.arcana-grid`: 3-column responsive grid (`repeat(auto-fill, minmax(340px, 1fr))`).
- Arcana cards: Remove modal trigger, render full description without truncation, display manifestation scaling effect clearly.
- Add results bar: `Showing X Arcana cards` + Sort/Filter reset.
- Prophecies: Dedicated filter and dark-glass Omen warning box (`background: rgba(239, 68, 68, 0.08); border: 1px solid rgba(239, 68, 68, 0.35);`).

- [ ] **Step 1: Write the failing tests**
  Assert that Arcana grid renders in 3 columns without requiring a modal overlay and that results bar counter is rendered.
- [ ] **Step 2: Run the test — verify it FAILS**
  `./mvnw test -Dtest=WikiDesignConsistencyTest`
- [ ] **Step 3: Implement Arcana and Prophecies cards**
  - Update `arcana-grid.html` to be self-contained (remove `is-clickable` and modal click, format description and manifestation box).
  - Update `prophecy-list.html` styling.
  - Update `arcana.html` and `prophecies.html`.
- [ ] **Step 4: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiDesignConsistencyTest+ArcanaWikiControllerTest`
- [ ] **Step 5: Commit**
  `git commit -m "style(arcana): upgrade to self-contained 3-column cards and dedicated prophecies view"`

---

### Task 4: Bestiary 2-Column Desktop / 1-Column Mobile & Quick-Glance Combat Cards

**Files:**
- Modify: `src/main/resources/templates/wiki/bestiary.html`
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Modify: `src/main/resources/templates/wiki/fragments/enemy-modal.html`
- Modify: `src/main/resources/static/css/main.css`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiControllerTest.java`

**Interfaces:**
- `.bestiary-grid`: 2-column desktop (`grid-template-columns: repeat(2, 1fr)`), 1-column mobile (`@media (max-width: 900px) { grid-template-columns: 1fr; }`).
- Enemy card anatomy:
  - Header: 64x64 portrait + Title + Gnosis badge (right) + Rank & Variant badges.
  - Center: **Combat Strip**: HP chip, Attack chip (`enemy.damage`), Key Elemental Resistances / Vulnerabilities summary pills (e.g. coral `Decay -100%`, slate `Fire +75%`).
  - Omit placeholder: Conditionally omit `"A denizen of the Witch domain."` or `"A denizen under the Witch command."`.
  - Footer: Location badge (`📍 Location (+N)`) + "Inspect Dossier →" action link.
- Move `Sort By` to the shared results bar above the grid for layout consistency.

- [ ] **Step 1: Write the failing tests**
  Add assertions checking:
  1. `enemy-grid.html` does not render the scraped filler string.
  2. `bestiary-grid` uses 2-column layout on desktop.
  3. `enemy-card` renders combat attributes (HP and attack damage if present).
- [ ] **Step 2: Run the test — verify it FAILS**
  `./mvnw test -Dtest=WikiDesignConsistencyTest`
- [ ] **Step 3: Implement Bestiary 2-column quick-glance card**
  - Update `enemy-grid.html` with combat stats and territory pill.
  - Update `bestiary.html` results bar and filter box.
  - Update `main.css` with responsive 2-col rules.
- [ ] **Step 4: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiDesignConsistencyTest+BestiaryWikiControllerTest`
- [ ] **Step 5: Commit**
  `git commit -m "feat(bestiary): convert to 2-column quick-glance combat cards and omit placeholder text"`

---

### Task 5: Full Verification & Visual Screenshot Re-capture

- [ ] **Step 1: Run complete Maven test suite**
  `./mvnw test`
- [ ] **Step 2: Re-capture screenshots of all 4 wiki tabs and modals**
  Capture fresh screenshots into artifact directory and verify visual harmony.
- [ ] **Step 3: Update documentation and task tracking**
- [ ] **Step 4: Commit**
  `git commit -m "docs: finalize wiki UI overhaul and verification screenshots"`
