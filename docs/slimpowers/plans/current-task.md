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
