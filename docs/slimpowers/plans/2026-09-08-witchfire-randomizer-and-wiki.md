# Witchfire Randomizer & Item Wiki Implementation Plan

**Goal:** Rewrite the Witchfire Item Wiki and Loadout Randomizer into Spring Boot 4, Thymeleaf, HTMX, Alpine.js, and custom dark fantasy CSS, with Melee Weapons support and automated wiki.gg data ingestion.

**Architecture:** Spring Boot backend with an in-memory `ItemRepository` populated from a scraped `items.json` seed file, `RandomizerService` implementing slot generation and lock rules, and Thymeleaf + HTMX + Alpine.js delivering fast server-rendered views and micro-interactions.

**Tech Stack:** Java 25, Spring Boot 4.1, Thymeleaf, HTMX 2.x, Alpine.js 3.x, JUnit 5, MockMvc, Custom CSS.

**Spec:** `docs/slimpowers/specs/2026-09-08-witchfire-randomizer-and-wiki-design.md`

## Global Constraints
- Java 25 floor, Spring Boot standard conventions.
- All equipment and loadouts must include the new `MeleeWeapon` category.
- Server-side partial DOM updates powered by HTMX; client-side toggles and modals powered by Alpine.js.
- Clean custom CSS without external Tailwind CLI or Node dependencies.
- Verification command: `./mvnw test`.

---

### Task 1: Scraper Script & Static Seed Assets

**Files:**
- Create: `scripts/scrape_wiki.py` (fetches Cargo data from `witchfire.wiki.gg`, parses Mysteria, downloads icons, writes `src/main/resources/data/items.json`)
- Create: `src/main/resources/data/items.json`
- Create: `src/main/resources/static/images/items/`
- Copy assets from old project: `src/main/resources/static/images/wf-bg.webP`, `src/main/resources/static/images/texture-transparent.PNG`, `src/main/resources/static/images/lockClosed.png`, `src/main/resources/static/images/lockOpen.png`

**Interfaces:**
- Consumes: `https://witchfire.wiki.gg/api.php` Cargo API
- Produces: Validated JSON seed containing weapons (including demonic), melee weapons, spells, magical items, and beads with local icon file references.

- [ ] **Step 1: Write scraper script**
  Create `scripts/scrape_wiki.py` with custom user agent, query logic for Cargo tables (`Weapons`, `MeleeWeapons`, `Spells`, `MagicalItems`, `Beads`), wikitext parser for `{{Mysteria}}`, and icon downloader.
- [ ] **Step 2: Run scraper script and copy base background images**
  Execute `python3 scripts/scrape_wiki.py` to populate `src/main/resources/data/items.json` and static image assets.
- [ ] **Step 3: Validate JSON structure**
  Verify `items.json` exists, is valid JSON, and contains entries across all categories (including Melee Weapons).
- [ ] **Step 4: Commit**
  `git add scripts/ src/main/resources/data/ src/main/resources/static/ && git commit -m "feat(data): add wiki scraper and initial items seed with melee weapons"`

---

### Task 2: Domain Model

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/ItemCategory.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Element.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/MysteriumTier.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/BeadRequirement.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Item.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Weapon.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/MeleeWeapon.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Bead.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Loadout.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/RandomizerRequest.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/model/ModelTest.java`

**Interfaces:**
- Produces: Strongly typed models for equipment items, weapons with stats, melee weapons with charge/special damage, beads with stat requirements, and the full loadout.

- [ ] **Step 1: Write failing test**
  Write `ModelTest.java` verifying creation, inheritance, and serialization of `Weapon`, `MeleeWeapon`, `Bead`, and `Loadout`.
- [ ] **Step 2: Run test — verify it fails**
  `./mvnw test -Dtest=ModelTest`
- [ ] **Step 3: Implement domain models**
  Implement the records/classes with Jackson annotations matching `items.json`.
- [ ] **Step 4: Run test — verify it passes**
  `./mvnw test -Dtest=ModelTest`
- [ ] **Step 5: Commit**
  `git add src/ && git commit -m "feat(model): add domain models for items, weapons, melee, beads, and loadout"`

---

### Task 3: Item Repository

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ItemRepository.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ItemRepositoryTest.java`

**Interfaces:**
- Consumes: `src/main/resources/data/items.json`
- Produces:
  - `List<Item> findAll()`
  - `Optional<Item> findById(String id)`
  - `List<Item> findByCategory(ItemCategory category)`
  - `List<Item> search(ItemCategory category, Element element, String query)`
  - `List<Bead> findEligibleBeads(Map<String, Integer> userStats)`

- [ ] **Step 1: Write failing test**
  Write `ItemRepositoryTest.java` verifying JSON loading, search by query/element/category, and bead stat validation.
- [ ] **Step 2: Run test — verify it fails**
  `./mvnw test -Dtest=ItemRepositoryTest`
- [ ] **Step 3: Implement `ItemRepository`**
  Implement Spring `@Repository` reading `data/items.json` at startup using Jackson `ObjectMapper`.
- [ ] **Step 4: Run test — verify it passes**
  `./mvnw test -Dtest=ItemRepositoryTest`
- [ ] **Step 5: Commit**
  `git add src/ && git commit -m "feat(repo): implement in-memory ItemRepository with filtering and bead constraints"`

---

### Task 4: Randomizer Service

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/service/RandomizerService.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/service/RandomizerServiceTest.java`

**Interfaces:**
- Consumes: `ItemRepository`
- Produces:
  - `Loadout generateRandomLoadout(RandomizerRequest request)`
  - `Item rerollSlot(String slotName, RandomizerRequest request)`

Rules enforced:
1. `primaryWeapon` and `secondaryWeapon` are distinct standard weapons (Close, Medium, Long).
2. `demonicWeapon` is from the Demonic range category.
3. `meleeWeapon` is from the `MELEE_WEAPON` category.
4. `lightSpell` and `heavySpell` are respective spell categories.
5. `relic`, `fetish`, `ring` are respective magical item categories.
6. Locked slots retain their incoming assigned items.
7. Excluded items are excluded unless locked.
8. Preferred elements increase selection probability or filter items when matching.
9. Selected beads respect user stat constraints and slot count.

- [ ] **Step 1: Write failing test**
  Write `RandomizerServiceTest.java` verifying slot distinctness, melee weapon inclusion, lock preservation, and bead restrictions.
- [ ] **Step 2: Run test — verify it fails**
  `./mvnw test -Dtest=RandomizerServiceTest`
- [ ] **Step 3: Implement `RandomizerService`**
  Implement the randomization algorithm with lock checks and filter rules.
- [ ] **Step 4: Run test — verify it passes**
  `./mvnw test -Dtest=RandomizerServiceTest`
- [ ] **Step 5: Commit**
  `git add src/ && git commit -m "feat(service): implement RandomizerService with slot locks and constraints"`

---

### Task 5: Custom Dark Fantasy CSS & Base Layout

**Files:**
- Create: `src/main/resources/static/css/main.css`
- Create: `src/main/resources/templates/layout/base.html`
- Create: `src/main/resources/templates/fragments/navigation.html`
- Download/vendor:
  - `src/main/resources/static/js/htmx.min.js`
  - `src/main/resources/static/js/alpine.min.js`

**Interfaces:**
- Produces: Unified layout with dark gothic theme, navigation bar (Wiki / Randomizer links), HTMX & Alpine scripts loaded locally for offline capability, responsive grid system, and CSS animations (`glow-pulse`, modal transitions, card hover effects).

- [ ] **Step 1: Vendor HTMX and Alpine.js locally**
  Place `htmx.min.js` and `alpine.min.js` in `src/main/resources/static/js/`.
- [ ] **Step 2: Create `main.css`**
  Write custom CSS matching Witchfire gothic styling, color tokens, parchment overlays, slot lock styles, and animations.
- [ ] **Step 3: Create base Thymeleaf template and navigation fragment**
  Structure `base.html` with `<head>`, `<nav th:replace="..."/>`, `<main layout:fragment="content"/>`, and modal containers.
- [ ] **Step 4: Verify static resources serve cleanly**
  `./mvnw test-compile`
- [ ] **Step 5: Commit**
  `git add src/main/resources/ && git commit -m "feat(ui): add custom dark fantasy css, vendored js, and base template"`

---

### Task 6: Item Wiki Web Layer

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiController.java`
- Create: `src/main/resources/templates/wiki/index.html`
- Create: `src/main/resources/templates/wiki/fragments/item-grid.html`
- Create: `src/main/resources/templates/wiki/fragments/item-card.html`
- Create: `src/main/resources/templates/wiki/fragments/item-modal.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiControllerTest.java`

**Routes:**
- `GET /wiki`: Renders full catalog with category tabs (Weapons, Melee, Light Spells, Heavy Spells, Relics, Fetishes, Rings, Beads).
- `GET /wiki/items`: HTMX endpoint returning `item-grid.html` filtered by `category`, `element`, `search`.
- `GET /wiki/item/{id}`: HTMX endpoint returning `item-modal.html` with full stats, weapon properties, and expandable Mysteria tiers.

- [ ] **Step 1: Write failing test**
  Write `WikiControllerTest.java` using `MockMvc` verifying `/wiki`, `/wiki/items`, and `/wiki/item/{id}` return 200 OK and expected HTML content.
- [ ] **Step 2: Run test — verify it fails**
  `./mvnw test -Dtest=WikiControllerTest`
- [ ] **Step 3: Implement `WikiController` and Thymeleaf templates**
  Implement controller and responsive templates with HTMX live search and Alpine modal integration.
- [ ] **Step 4: Run test — verify it passes**
  `./mvnw test -Dtest=WikiControllerTest`
- [ ] **Step 5: Commit**
  `git add src/ && git commit -m "feat(wiki): implement wiki controller, live filters, and item modal"`

---

### Task 7: Randomizer Web Layer

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerController.java`
- Create: `src/main/resources/templates/randomizer/index.html`
- Create: `src/main/resources/templates/randomizer/fragments/loadout-grid.html`
- Create: `src/main/resources/templates/randomizer/fragments/slot-card.html`
- Create: `src/main/resources/templates/randomizer/fragments/bead-settings-modal.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Routes:**
- `GET /` & `GET /randomizer`: Renders randomizer page with initial generated loadout.
- `POST /randomizer/reroll`: HTMX endpoint receiving locked slot states and filters, returns updated `loadout-grid.html`.
- `POST /randomizer/reroll-slot`: HTMX endpoint rerolling an individual slot.

**Alpine.js micro-interactions:**
- Slot lock toggling with visual lock icon swap and input form state sync.
- Bead stat sliders modal with instant requirement check.
- "Share Loadout" button copying URL with query parameters and showing a brief animated toast.

- [ ] **Step 1: Write failing test**
  Write `RandomizerControllerTest.java` using `MockMvc` testing initial render and POST reroll with locked slots.
- [ ] **Step 2: Run test — verify it fails**
  `./mvnw test -Dtest=RandomizerControllerTest`
- [ ] **Step 3: Implement `RandomizerController` and templates**
  Implement controller, slots layout (Primary, Secondary, Demonic, Melee, Spells, Magical Items, Beads), lock mechanics, and share link generator.
- [ ] **Step 4: Run test — verify it passes**
  `./mvnw test -Dtest=RandomizerControllerTest`
- [ ] **Step 5: Commit**
  `git add src/ && git commit -m "feat(randomizer): implement randomizer controller, HTMX rerolls, and Alpine lock state"`

---

### Task 8: Verification & Polish

**Files:**
- Modify: `AGENTS.md` (Update architecture, endpoints, commands if needed)
- Test: Full test suite `./mvnw test`

- [ ] **Step 1: Run complete test suite**
  Execute `./mvnw test` and verify 100% pass rate.
- [ ] **Step 2: Start application and verify web endpoints**
  Verify static assets, wiki search, and randomizer rerolls in a live run.
- [ ] **Step 3: Commit and final report**
  `git commit -m "chore: final verification and documentation updates"`
