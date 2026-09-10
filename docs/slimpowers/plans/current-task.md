# Current Task: Arcana, Prophecies & Bestiary Wiki Expansion

- [x] Task 1: Data Ingestion & Scraper Extension (Arcana, Prophecies, Enemies)
  - [x] Step 1: Write test validation script
  - [x] Step 2: Run validation script — verify it FAILS
  - [x] Step 3: Implement scraper logic and download data/icons
  - [x] Step 4: Run validation script — verify it PASSES
  - [x] Step 5: Commit
- [x] Task 2: Arcana & Prophecy Domain Models and ArcanaRepository
  - [x] Step 1: Write failing test (`ArcanaRepositoryTest`)
  - [x] Step 2: Run test — verify it FAILS
  - [x] Step 3: Implement `Arcana`, `Prophecy`, and `ArcanaRepository`
  - [x] Step 4: Run test — verify it PASSES
  - [x] Step 5: Commit
- [x] Task 3: Enemy Domain Model and EnemyRepository
  - [x] Step 1: Write failing test (`EnemyRepositoryTest`)
  - [x] Step 2: Run test — verify it FAILS
  - [x] Step 3: Implement `Enemy` and `EnemyRepository`
  - [x] Step 4: Run test — verify it PASSES
  - [x] Step 5: Commit
- [x] Task 4: Wiki Sub-Navigation Bar & Sidebar Drawer Update
  - [x] Step 1: Write failing test (`WikiNavigationTest`)
  - [x] Step 2: Run test — verify it FAILS
  - [x] Step 3: Implement sub-nav fragment and update navigation
  - [x] Step 4: Run test — verify it PASSES
  - [x] Step 5: Commit
- [x] Task 5: Arcana & Prophecies View and Controller Endpoints
  - [x] Step 1: Write failing test (`ArcanaWikiControllerTest`)
  - [x] Step 2: Run test — verify it FAILS
  - [x] Step 3: Implement controller and templates
  - [x] Step 4: Run test — verify it PASSES
  - [x] Step 5: Commit
- [x] Task 6: Bestiary View and Controller Endpoints
  - [x] Step 1: Write failing test (`BestiaryWikiControllerTest`)
  - [x] Step 2: Run test — verify it FAILS
  - [x] Step 3: Implement controller and templates
  - [x] Step 4: Run test — verify it PASSES
  - [x] Step 5: Commit
- [x] Task 7: CSS Styling Polish & Full Suite Verification
  - [x] Step 1: Add dark-fantasy styles in `main.css` (no simple emojis)
  - [x] Step 2: Run full verification `./mvnw test`
  - [x] Step 3: Commit

---

# Current Task: Wiki UI Harmonization

Plan: `docs/slimpowers/plans/2026-09-10-wiki-ui-harmonization.md`

- [x] Task 1: Wiki design-consistency regression tests (`WikiDesignConsistencyTest`)
  - [x] Step 1: Write failing tests
  - [x] Step 2: Run tests — verify 8 of 9 FAIL
  - [x] Step 3: Commit
- [x] Task 2: One gold token + app font (drop the undefined `--font-gothic` serif)
- [x] Task 3: Card surfaces back to the shared `.texture-box` glass + app title treatment
- [x] Task 4: Wiki sub-nav to the app underline-tab idiom; fix the 9 px mobile overflow
- [x] Task 5: Flatten modal content; revert the shared `.detail-modal-box` regression
- [x] Task 6: Shared footer fragment, search gap, bestiary results header, stale copy
- [x] Task 7: Full verification
  - [x] Step 1: `./mvnw test` → 79 tests, 0 failures
  - [x] Step 2: Re-capture screenshots (`target/ui-review/shots-after/`) and layout probe
  - [x] Step 3: Commit

---

# Current Task: Wiki UI Refinements (De-cluttering & Color Calming)

Plan: `docs/slimpowers/plans/2026-09-10-wiki-ui-refinements.md`

- [x] Task 1: Refinement regression tests (`WikiDesignConsistencyTest`)
  - [x] Step 1: Write failing tests
  - [x] Step 2: Run tests — verify FAIL
  - [x] Step 3: Commit
- [x] Task 2: Bestiary card de-cluttering & palette calming
- [x] Task 3: Bestiary modal harmonization (Gothic compendium aesthetic)
- [x] Task 4: Arcana filter streamlining & tag deduplication
- [x] Task 5: Full verification & screenshot re-capture

---

# Current Task: Wiki Layout Harmonization & Bestiary Redesign

Plan: `docs/slimpowers/plans/2026-09-10-wiki-layout-harmonization-and-bestiary-redesign.md`

- [x] Task 1: Navigation & Controller Routing Expansion (4 Direct Tabs)
  - [x] Created `PropheciesWikiController` (`/wiki/prophecies`, `/wiki/prophecies/list`)
  - [x] Sub-nav and sidebar drawer updated to 4 tabs: Equipment, Arcana, Prophecies, Bestiary
  - [x] Created dedicated `templates/wiki/prophecies.html`
  - [x] Tests passing & committed (`ec3d493`)
- [x] Task 2: Equipment 2-Column Grid & Mysterium Polish
  - [x] 2-column grid (`repeat(2, 1fr)`) on desktop, 1-column mobile
  - [x] Dark-glass Mysterium level header styling with gold accent
  - [x] Centered search clear button and added Reset Filters button
  - [x] Tests passing & committed (`a0980cf`)
- [x] Task 3: Self-Contained 3-Column Arcana & Refined Prophecies
  - [x] 3-column responsive grid (`minmax(340px, 1fr)`)
  - [x] Self-contained cards (manifestation scaling + description directly visible)
  - [x] Hollow modal removed; dedicated Prophecies view with direct links to Arcana
  - [x] Tests passing & committed (`1323a70`)
- [x] Task 4: Bestiary 2-Column Quick-Glance Combat Cards
  - [x] 2-column desktop / 1-column mobile grid
  - [x] Combat stats strip (HP, ATK), active resistance pills, territory chips
  - [x] Omitted scraped placeholder filler text
  - [x] Removed redundant modal overlay; aligned Sort By to results header
  - [x] Tests passing & committed (`0b5b64b`)
- [x] Task 5: Visual Verification & Test Suite Verification
  - [x] Captured fresh full-page and mobile screenshots across all 4 tabs
  - [x] Full test suite verified: `./mvnw test` (90 tests passed, 0 failures)


