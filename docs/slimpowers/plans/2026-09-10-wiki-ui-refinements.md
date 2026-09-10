# Wiki UI Refinements Implementation Plan

**Goal:** De-clutter Arcana, Prophecies, and Bestiary cards and modals, remove the rainbow color dissonance in the Bestiary, and fully harmonize the wiki with the dark fantasy Gothic aesthetic of the app.

**Architecture:** Presentation and template refinements:
1. Re-style Bestiary health and combat chips to dark-glass neutral/gold tokens (eliminating neon green `#34d399` / `#10b981`).
2. Remove card-face resistance badge noise and verbose multi-line location lists from `enemy-grid.html`, keeping cards clean and breathable.
3. Replace the SaaS-style chunky `.res-card` dashboard blocks in the Bestiary modal with an elegant dark fantasy stats grid matching `.wiki-stats-grid`.
4. Replace the 17 wrapping category pills in `arcana.html` with a clean themed dropdown `<select>`, matching Bestiary's Location/Sort filters.
5. Deduplicate tags on Arcana cards and flatten the Manifestation Scaling block in the Arcana modal.

**Tech Stack:** Spring Boot 4 / Java, Thymeleaf, HTMX, Alpine.js, CSS, JUnit 5 + MockMvc.

**Spec:** `/home/hendrik/.gemini/antigravity-cli/brain/139fab4c-38d9-404e-9725-10a1a135a4c5/wiki-ui-review-and-optimization-proposal.md`

## Global Constraints

- Do not edit `.mvn/wrapper/*` or generated build artifacts.
- Verify (everything): `./mvnw test`; single test: `./mvnw test -Dtest=TestClassName#testMethodName`.
- No new CSS framework, no new web font, no new JS dependency.
- All colors must adhere to the app's dark fantasy palette and `:root` tokens.
- All Thymeleaf fragments and Alpine.js/HTMX bindings must remain functional.

---

### Task 1: Refinement Regression Tests

**Files:**
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: `main.css`, `enemy-grid.html`, `arcana.html`.
- Produces: automated assertions that guard against neon colors, card resistance clutter, and pill sprawl.

- [ ] **Step 1: Write the failing tests**
  Add assertions checking:
  1. `main.css` does not use `#34d399` or `16, 185, 129` (neon emerald).
  2. `enemy-grid.html` does not render `.resistance-badges-container` or `.weak-fire`.
  3. `arcana.html` renders a dropdown select for prophecy categories instead of 17 pills.
- [ ] **Step 2: Run the test — verify it FAILS**
  `./mvnw test -Dtest=WikiDesignConsistencyTest`
- [ ] **Step 3: Implement** — Done in Tasks 2–4.
- [ ] **Step 4: Run the test — verify it PASSES**
- [ ] **Step 5: Commit**
  `git commit -m "test(ui): add tests for wiki clutter reduction and color harmonization"`

---

### Task 2: Bestiary Card De-cluttering & Palette Calming

**Files:**
- Modify: `src/main/resources/static/css/main.css`
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Test: `WikiDesignConsistencyTest`

**Interfaces:**
- Consumes: `.texture-box`, `.rank-badge`.
- Produces: a calm, breathable Bestiary card without rainbow badges or 3-line location strings.

- [ ] **Step 1: Run the test — verify it FAILS**
- [ ] **Step 2: Implement**
  - In `enemy-grid.html`:
    - Remove the `.resistance-badges-container` block.
    - Condense the locations list into a clean single-line summary (first location or territory count).
  - In `main.css`:
    - Update `.health-chip` to use `rgba(0, 0, 0, 0.35)` background, `border: 1px solid var(--wf-border-subtle)`, and `#d1d5db` or `--wf-gold-bright` text.
- [ ] **Step 3: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiDesignConsistencyTest+BestiaryWikiControllerTest`
- [ ] **Step 4: Commit**
  `git commit -m "style(bestiary): de-clutter enemy cards and calm health badge color"`

---

### Task 3: Bestiary Modal Harmonization (Gothic Compendium Aesthetic)

**Files:**
- Modify: `src/main/resources/static/css/main.css`
- Modify: `src/main/resources/templates/wiki/fragments/enemy-modal.html`
- Test: `WikiDesignConsistencyTest`

**Interfaces:**
- Consumes: `.wiki-stats-grid` convention.
- Produces: an elegant resistance and combat stats layout inside the Enemy Detail Modal.

- [ ] **Step 1: Run the test — verify it FAILS**
- [ ] **Step 2: Implement**
  - In `main.css`:
    - Refactor `.resistance-cards-grid` and `.res-card` into a refined, compact grid.
    - Style `.res-vulnerable`, `.res-resistant`, `.res-neutral` with dark-glass backgrounds and subtle text accents (soft coral `#f87171` for weak, slate `#94a3b8` for resistant, muted gray `#6b7280` for neutral).
    - Update `.enemy-stat-chip.health-chip` and `.enemy-stat-chip.attack-chip` to clean dark-glass tokens.
  - In `enemy-modal.html`:
    - Clean up combat strip and resistance grid layout.
- [ ] **Step 3: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiDesignConsistencyTest+BestiaryWikiControllerTest`
- [ ] **Step 4: Commit**
  `git commit -m "style(bestiary): harmonize enemy modal stats and resistances with gothic aesthetic"`

---

### Task 4: Arcana Filter Streamlining & Tag Deduplication

**Files:**
- Modify: `src/main/resources/templates/wiki/arcana.html`
- Modify: `src/main/resources/templates/wiki/fragments/arcana-grid.html`
- Modify: `src/main/resources/templates/wiki/fragments/arcana-modal.html`
- Modify: `src/main/resources/static/css/main.css`
- Test: `WikiDesignConsistencyTest`, `ArcanaWikiControllerTest`

**Interfaces:**
- Consumes: `prophecyTypes` model attribute, Alpine `selectedProphecyType`.
- Produces: a compact filter box and clean Arcana cards/modal.

- [ ] **Step 1: Run the test — verify it FAILS**
- [ ] **Step 2: Implement**
  - In `arcana.html`:
    - Replace the 17-pill `filter-pills-row` for categories with a clean `<select class="custom-select">` bound to `selectedProphecyType`.
  - In `arcana-grid.html`:
    - Filter out the prophecy type tag if it matches the card's element name (preventing duplicate `● Fire` and `Fire Element`).
  - In `arcana-modal.html`:
    - Remove the artificial `.arcana-scaling-header` (`Enchantment Tiers | MANIFESTATION POWER`) and present the scaling power cleanly with warm gold typography.
- [ ] **Step 3: Run the test — verify it PASSES**
  `./mvnw test -Dtest=WikiDesignConsistencyTest+ArcanaWikiControllerTest`
- [ ] **Step 4: Commit**
  `git commit -m "style(arcana): streamline category filter into dropdown and deduplicate card tags"`

---

### Task 5: Full Verification & Visual Evidence Re-capture

- [ ] **Step 1: Run full test suite**
  `./mvnw test`
- [ ] **Step 2: Re-capture screenshots**
  Capture fresh screenshots of Bestiary grid, Bestiary modal, Arcana grid, and Arcana modal.
- [ ] **Step 3: Update task tracking**
- [ ] **Step 4: Commit**
