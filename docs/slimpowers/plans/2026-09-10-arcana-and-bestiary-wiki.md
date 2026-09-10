# Arcana, Prophecies & Bestiary Wiki Expansion Implementation Plan

**Goal:** Expand the Witchfire companion wiki with an Arcana & Prophecies Compendium and a Bestiary & Vulnerability Guide, unified by a clean sub-navigation bar adhering strictly to the dark-fantasy design aesthetic without simple emojis.

**Architecture:** Extend `scripts/scrape_wiki.py` to scrape Arcana, Prophecies, and Enemies into bundled JSON datasets. Build dedicated `ArcanaRepository` and `EnemyRepository` services loading datasets on startup. Serve dynamic Thymeleaf pages and HTMX live-filtering fragments via `WikiController` with Alpine.js modals.

**Tech Stack:** Java 25, Spring Boot 4.1 (`spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf`), Thymeleaf, HTMX, Alpine.js, JUnit 5 / MockMvc, Python 3 (offline data ingestion).

**Spec:** `docs/slimpowers/specs/2026-09-10-arcana-and-bestiary-wiki-design.md`

## Global Constraints
- Strictly no simple emojis in the UI (no ⚔️, 🔮, 💀, 🔥, ⚡, 💧, 🌿). Use clean typography, styled CSS pill badges, and subtle SVG icons.
- Preserve existing dark-fantasy color palette: dark backgrounds (`#0e0d0b`, `#161412`), gold accents (`#d4af37`), and element colors (`#ff4d4d` Fire, `#4da6ff` Water, `#66cc66` Earth, `#ffcc00` Air).
- All new controllers must support both full-page loads and HTMX partial fragment swaps.
- Before claiming completion, verify full test suite passes with `./mvnw test`.

---

## Task Structure

### Task 1: Data Ingestion & Scraper Extension

**Files:**
- Modify: `scripts/scrape_wiki.py` — add methods to query Cargo tables for Arcana, Prophecies, Omens, and Enemy, and download icons
- Output: `src/main/resources/data/arcana.json`
- Output: `src/main/resources/data/prophecies.json`
- Output: `src/main/resources/data/enemies.json`
- Test: Run validation script checking JSON validity and entry counts

**Interfaces:**
- Consumes: MediaWiki Cargo API (`action=cargoquery`) on `witchfire.wiki.gg`
- Produces: Valid JSON files in `src/main/resources/data/` and local icons in `src/main/resources/static/images/`

- [ ] **Step 1: Write test validation script**
  Create `scripts/validate_scraped_data.py`:
  ```python
  import json, sys

  for fpath, min_count in [
      ("src/main/resources/data/arcana.json", 100),
      ("src/main/resources/data/prophecies.json", 15),
      ("src/main/resources/data/enemies.json", 50),
  ]:
      try:
          with open(fpath, "r", encoding="utf-8") as f:
              data = json.load(f)
              assert len(data) >= min_count, f"{fpath} has {len(data)} items, expected >= {min_count}"
              print(f"PASS: {fpath} loaded {len(data)} items")
      except Exception as e:
          print(f"FAIL: {fpath} - {e}")
          sys.exit(1)
  ```

- [ ] **Step 2: Run validation script — verify it FAILS**
  Command: `python3 scripts/validate_scraped_data.py`
  Expected output: FAIL (files do not exist yet)

- [ ] **Step 3: Update scraper implementation**
  Add functions in `scripts/scrape_wiki.py`:
  - `scrape_arcana()`: Query `Arcana` table (`name`, `prophecyTypes`, `description`, `effects`), clean wikitext, map element, download image or set fallback.
  - `scrape_prophecies()`: Query `Prophecies` and `Omens` tables, join omen effect to prophecy by `omenName`.
  - `scrape_enemies()`: Query `Enemy` table (`name`, `description`, `rank`, `gnosis`, `health`, `damage`, `variants`, `location`, resistances), clean location links, download portraits.
  - Execute scraper to populate `arcana.json`, `prophecies.json`, and `enemies.json`.

- [ ] **Step 4: Run validation script — verify it PASSES**
  Command: `python3 scripts/validate_scraped_data.py`
  Expected output: PASS: all three JSON files loaded with expected counts.

- [ ] **Step 5: Commit**
  `git add scripts/ src/main/resources/data/ src/main/resources/static/images/ && git commit -m "feat(data): scrape and bundle Arcana, Prophecies, and Bestiary data"`

---

### Task 2: Arcana & Prophecy Domain Models and ArcanaRepository

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Arcana.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Prophecy.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ArcanaRepository.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ArcanaRepositoryTest.java`

**Interfaces:**
- Consumes: `arcana.json` and `prophecies.json` from classpath
- Produces: `ArcanaRepository` methods:
  - `List<Arcana> findAll()`
  - `Optional<Arcana> findById(String id)`
  - `List<Arcana> search(String query, String prophecyType, Element element)`
  - `List<Prophecy> findAllProphecies()`
  - `Optional<Prophecy> findProphecyById(String id)`
  - `List<Arcana> findArcanaByProphecy(String prophecyId)`

- [ ] **Step 1: Write the failing test**
  Create `ArcanaRepositoryTest.java`:
  ```java
  package dev.hendrikhoemberg.witchfirerandomizer.repository;

  import dev.hendrikhoemberg.witchfirerandomizer.model.Arcana;
  import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
  import dev.hendrikhoemberg.witchfirerandomizer.model.Prophecy;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  import java.util.List;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;

  class ArcanaRepositoryTest {

      private ArcanaRepository repository;

      @BeforeEach
      void setUp() {
          repository = new ArcanaRepository();
          repository.init();
      }

      @Test
      void shouldLoadArcanaAndPropheciesFromClasspath() {
          assertThat(repository.findAll()).isNotEmpty();
          assertThat(repository.findAllProphecies()).isNotEmpty();
      }

      @Test
      void shouldFindArcanaById() {
          Optional<Arcana> accelerant = repository.findById("arcana-accelerant");
          assertThat(accelerant).isPresent();
          assertThat(accelerant.get().getName()).isEqualTo("Accelerant");
      }

      @Test
      void shouldSearchArcanaByKeywordAndElement() {
          List<Arcana> results = repository.search("burning", null, Element.Fire);
          assertThat(results).isNotEmpty();
          assertThat(results).allMatch(a -> a.getElement() == Element.Fire);
      }

      @Test
      void shouldLinkProphecyToAssociatedArcana() {
          Optional<Prophecy> fireProphecy = repository.findProphecyById("prophecy-prophecy-of-fire");
          assertThat(fireProphecy).isPresent();
          List<Arcana> linked = repository.findArcanaByProphecy("prophecy-prophecy-of-fire");
          assertThat(linked).isNotEmpty();
      }
  }
  ```

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=ArcanaRepositoryTest`
  Expected output: Compilation failure (classes do not exist yet)

- [ ] **Step 3: Write minimal implementation**
  - Implement `Arcana.java` (record or class with Jackson annotations):
    `id`, `name`, `description`, `effects`, `prophecyTypes`, `element`, `iconUrl`.
  - Implement `Prophecy.java`:
    `id`, `name`, `description`, `arcanaType`, `omenName`, `omenEffect`, `location`, `iconUrl`.
  - Implement `ArcanaRepository.java` annotated with `@Component`:
    Loads `/data/arcana.json` and `/data/prophecies.json` in `@PostConstruct init()`.

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=ArcanaRepositoryTest`
  Expected output: PASS (all tests pass)

- [ ] **Step 5: Commit**
  `git add src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Arcana.java src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Prophecy.java src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ArcanaRepository.java src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ArcanaRepositoryTest.java && git commit -m "feat(repo): add Arcana and Prophecy domain models and ArcanaRepository"`

---

### Task 3: Enemy Domain Model and EnemyRepository

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Enemy.java`
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/EnemyRepository.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/EnemyRepositoryTest.java`

**Interfaces:**
- Consumes: `enemies.json` from classpath
- Produces: `EnemyRepository` methods:
  - `List<Enemy> findAll()`
  - `Optional<Enemy> findById(String id)`
  - `List<Enemy> search(String query, Integer gnosis, String rank, String location, String sort)`
  - `List<String> getAllLocations()`
  - `List<String> getAllRanks()`

- [ ] **Step 1: Write the failing test**
  Create `EnemyRepositoryTest.java`:
  ```java
  package dev.hendrikhoemberg.witchfirerandomizer.repository;

  import dev.hendrikhoemberg.witchfirerandomizer.model.Enemy;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  import java.util.List;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;

  class EnemyRepositoryTest {

      private EnemyRepository repository;

      @BeforeEach
      void setUp() {
          repository = new EnemyRepository();
          repository.init();
      }

      @Test
      void shouldLoadEnemiesFromClasspath() {
          assertThat(repository.findAll()).isNotEmpty();
      }

      @Test
      void shouldFilterByGnosisLevel() {
          List<Enemy> gnosis3 = repository.search(null, 3, null, null, "name");
          assertThat(gnosis3).isNotEmpty();
          assertThat(gnosis3).allMatch(e -> e.getGnosis() != null && e.getGnosis() == 3);
      }

      @Test
      void shouldFilterByRank() {
          List<Enemy> faithful = repository.search(null, null, "Faithful", null, "name");
          assertThat(faithful).isNotEmpty();
          assertThat(faithful).allMatch(e -> "Faithful".equalsIgnoreCase(e.getRank()));
      }

      @Test
      void shouldSortByHealthDescending() {
          List<Enemy> sorted = repository.search(null, null, null, null, "health");
          assertThat(sorted).isNotEmpty();
          for (int i = 0; i < sorted.size() - 1; i++) {
              assertThat(sorted.get(i).getHealth()).isGreaterThanOrEqualTo(sorted.get(i + 1).getHealth());
          }
      }
  }
  ```

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=EnemyRepositoryTest`
  Expected output: Compilation failure (Enemy / EnemyRepository not found)

- [ ] **Step 3: Write minimal implementation**
  - Implement `Enemy.java` with Jackson annotations and resistance convenience methods:
    `id`, `name`, `description`, `rank`, `gnosis`, `health`, `damage`, `variants`, `locations`, `fireResistance`, `earthResistance`, `waterResistance`, `airResistance`, `burnResistance`, `decayResistance`, `freezeResistance`, `shockResistance`, `stunResistance`, `staggerResistance`, `iconUrl`.
  - Implement `EnemyRepository.java` annotated with `@Component`:
    Loads `/data/enemies.json` in `@PostConstruct init()`.

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=EnemyRepositoryTest`
  Expected output: PASS (all tests pass)

- [ ] **Step 5: Commit**
  `git add src/main/java/dev/hendrikhoemberg/witchfirerandomizer/model/Enemy.java src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/EnemyRepository.java src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/EnemyRepositoryTest.java && git commit -m "feat(repo): add Enemy model and EnemyRepository with filtering and sorting"`

---

### Task 4: Wiki Sub-Navigation Bar & Sidebar Drawer Update

**Files:**
- Create: `src/main/resources/templates/wiki/fragments/wiki-nav.html` — reusable sub-navigation tab bar (Equipment, Arcana & Prophecies, Bestiary)
- Modify: `src/main/resources/templates/fragments/navigation.html` — update mobile sidebar drawer links
- Modify: `src/main/resources/templates/wiki/index.html` — include sub-navigation tab bar
- Modify: `src/main/resources/static/css/main.css` — styling for wiki sub-navigation tabs (clean text, active indicator, no emojis)
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiNavigationTest.java`

**Interfaces:**
- Produces: Shared sub-navigation bar `wikiNav(currentSection)` fragment with active state highlight and responsive styling.

- [ ] **Step 1: Write the failing test**
  Create `WikiNavigationTest.java`:
  ```java
  package dev.hendrikhoemberg.witchfirerandomizer.controller;

  import dev.hendrikhoemberg.witchfirerandomizer.repository.ItemRepository;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
  import org.springframework.test.context.bean.override.mockito.MockitoBean;
  import org.springframework.test.web.servlet.MockMvc;

  import static org.hamcrest.Matchers.containsString;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  @WebMvcTest(WikiController.class)
  class WikiNavigationTest {

      @Autowired
      private MockMvc mockMvc;

      @MockitoBean
      private ItemRepository itemRepository;

      @Test
      void shouldRenderSubNavWithEquipmentArcanaAndBestiaryTabs() throws Exception {
          mockMvc.perform(get("/wiki"))
                  .andExpect(status().isOk())
                  .andExpect(content().string(containsString("Equipment")))
                  .andExpect(content().string(containsString("/wiki/arcana")))
                  .andExpect(content().string(containsString("/wiki/bestiary")));
      }
  }
  ```

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=WikiNavigationTest`
  Expected output: FAIL (tab links not present in `/wiki` yet)

- [ ] **Step 3: Write minimal implementation**
  - Create `src/main/resources/templates/wiki/fragments/wiki-nav.html`:
    ```html
    <!DOCTYPE html>
    <html xmlns:th="http://www.thymeleaf.org">
    <body>
    <div th:fragment="wikiNav(activeSection)" class="wiki-subnav-container">
        <div class="wiki-subnav-tabs">
            <a href="/wiki" class="wiki-tab-link" th:classappend="${activeSection == 'equipment' ? 'active' : ''}">Equipment</a>
            <a href="/wiki/arcana" class="wiki-tab-link" th:classappend="${activeSection == 'arcana' ? 'active' : ''}">Arcana &amp; Prophecies</a>
            <a href="/wiki/bestiary" class="wiki-tab-link" th:classappend="${activeSection == 'bestiary' ? 'active' : ''}">Bestiary</a>
        </div>
    </div>
    </body>
    </html>
    ```
  - Include the fragment in `src/main/resources/templates/wiki/index.html` below the logo.
  - Update `src/main/resources/templates/fragments/navigation.html` to add direct links to `/wiki`, `/wiki/arcana`, and `/wiki/bestiary`.
  - Add CSS styles in `main.css` for `.wiki-subnav-tabs` and `.wiki-tab-link`.

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=WikiNavigationTest`
  Expected output: PASS

- [ ] **Step 5: Commit**
  `git add src/main/resources/templates/wiki/fragments/wiki-nav.html src/main/resources/templates/fragments/navigation.html src/main/resources/templates/wiki/index.html src/main/resources/static/css/main.css src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiNavigationTest.java && git commit -m "feat(ui): add clean wiki sub-navigation bar and update sidebar drawer"`

---

### Task 5: Arcana & Prophecies View and Controller Endpoints

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/ArcanaWikiController.java`
- Create: `src/main/resources/templates/wiki/arcana.html`
- Create: `src/main/resources/templates/wiki/fragments/arcana-grid.html`
- Create: `src/main/resources/templates/wiki/fragments/arcana-modal.html`
- Create: `src/main/resources/templates/wiki/fragments/prophecy-list.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/ArcanaWikiControllerTest.java`

**Interfaces:**
- Endpoints:
  - `GET /wiki/arcana` -> Full page render
  - `GET /wiki/arcana/cards` -> HTMX fragment `arcana-grid :: arcanaGrid`
  - `GET /wiki/arcana/card/{id}` -> HTMX fragment `arcana-modal :: arcanaModalContent`
  - `GET /wiki/arcana/prophecies` -> HTMX fragment `prophecy-list :: prophecyList`

- [ ] **Step 1: Write the failing test**
  Create `ArcanaWikiControllerTest.java`:
  ```java
  package dev.hendrikhoemberg.witchfirerandomizer.controller;

  import dev.hendrikhoemberg.witchfirerandomizer.model.Arcana;
  import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
  import dev.hendrikhoemberg.witchfirerandomizer.repository.ArcanaRepository;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
  import org.springframework.test.context.bean.override.mockito.MockitoBean;
  import org.springframework.test.web.servlet.MockMvc;

  import java.util.List;
  import java.util.Optional;

  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.when;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

  @WebMvcTest(ArcanaWikiController.class)
  class ArcanaWikiControllerTest {

      @Autowired
      private MockMvc mockMvc;

      @MockitoBean
      private ArcanaRepository arcanaRepository;

      @Test
      void shouldRenderArcanaPage() throws Exception {
          when(arcanaRepository.findAll()).thenReturn(List.of(new Arcana("arcana-test", "Test Card", "Desc", "Effect", List.of(), Element.Fire, "/icon.png")));
          when(arcanaRepository.findAllProphecies()).thenReturn(List.of());

          mockMvc.perform(get("/wiki/arcana"))
                  .andExpect(status().isOk())
                  .andExpect(view().name("wiki/arcana"))
                  .andExpect(model().attributeExists("cards", "prophecies"));
      }

      @Test
      void shouldReturnCardGridFragment() throws Exception {
          when(arcanaRepository.search(any(), any(), any())).thenReturn(List.of());

          mockMvc.perform(get("/wiki/arcana/cards"))
                  .andExpect(status().isOk())
                  .andExpect(view().name("wiki/fragments/arcana-grid :: arcanaGrid"));
      }

      @Test
      void shouldReturnCardModalFragment() throws Exception {
          Arcana card = new Arcana("arcana-test", "Test Card", "Desc", "Effect", List.of(), Element.Fire, "/icon.png");
          when(arcanaRepository.findById("arcana-test")).thenReturn(Optional.of(card));

          mockMvc.perform(get("/wiki/arcana/card/arcana-test"))
                  .andExpect(status().isOk())
                  .andExpect(view().name("wiki/fragments/arcana-modal :: arcanaModalContent"))
                  .andExpect(model().attribute("card", card));
      }

      @Test
      void shouldReturn404ForMissingCard() throws Exception {
          when(arcanaRepository.findById("missing")).thenReturn(Optional.empty());

          mockMvc.perform(get("/wiki/arcana/card/missing"))
                  .andExpect(status().isNotFound());
      }
  }
  ```

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=ArcanaWikiControllerTest`
  Expected output: Compilation failure (controller does not exist yet)

- [ ] **Step 3: Write minimal implementation**
  - Implement `ArcanaWikiController.java` with `@Controller` and `@RequestMapping("/wiki/arcana")`.
  - Implement `arcana.html`, `arcana-grid.html`, `arcana-modal.html`, and `prophecy-list.html` with:
    - Debounced HTMX search input
    - Element and Category filter pills (clean text, no emojis)
    - View toggle between "Arcana Cards" and "Prophecies & Omens"
    - Prophecy card with highlighted Omen debuff section and "View Arcana Pool" action
    - Card modal with Alpine.js overlay.

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=ArcanaWikiControllerTest`
  Expected output: PASS

- [ ] **Step 5: Commit**
  `git add src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/ArcanaWikiController.java src/main/resources/templates/wiki/arcana.html src/main/resources/templates/wiki/fragments/arcana-*.html src/main/resources/templates/wiki/fragments/prophecy-*.html src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/ArcanaWikiControllerTest.java && git commit -m "feat(wiki): add Arcana & Prophecies catalog views and controller endpoints"`

---

### Task 6: Bestiary View and Controller Endpoints

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiController.java`
- Create: `src/main/resources/templates/wiki/bestiary.html`
- Create: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Create: `src/main/resources/templates/wiki/fragments/enemy-modal.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiControllerTest.java`

**Interfaces:**
- Endpoints:
  - `GET /wiki/bestiary` -> Full page render
  - `GET /wiki/bestiary/enemies` -> HTMX fragment `enemy-grid :: enemyGrid`
  - `GET /wiki/bestiary/enemy/{id}` -> HTMX fragment `enemy-modal :: enemyModalContent`

- [ ] **Step 1: Write the failing test**
  Create `BestiaryWikiControllerTest.java`:
  ```java
  package dev.hendrikhoemberg.witchfirerandomizer.controller;

  import dev.hendrikhoemberg.witchfirerandomizer.model.Enemy;
  import dev.hendrikhoemberg.witchfirerandomizer.repository.EnemyRepository;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
  import org.springframework.test.context.bean.override.mockito.MockitoBean;
  import org.springframework.test.web.servlet.MockMvc;

  import java.util.List;
  import java.util.Optional;

  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.when;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

  @WebMvcTest(BestiaryWikiController.class)
  class BestiaryWikiControllerTest {

      @Autowired
      private MockMvc mockMvc;

      @MockitoBean
      private EnemyRepository enemyRepository;

      @Test
      void shouldRenderBestiaryPage() throws Exception {
          when(enemyRepository.findAll()).thenReturn(List.of());
          when(enemyRepository.getAllRanks()).thenReturn(List.of("Minor", "Faithful"));
          when(enemyRepository.getAllLocations()).thenReturn(List.of("Irongate Castle"));

          mockMvc.perform(get("/wiki/bestiary"))
                  .andExpect(status().isOk())
                  .andExpect(view().name("wiki/bestiary"))
                  .andExpect(model().attributeExists("enemies", "ranks", "locations"));
      }

      @Test
      void shouldReturnEnemyGridFragment() throws Exception {
          when(enemyRepository.search(any(), any(), any(), any(), any())).thenReturn(List.of());

          mockMvc.perform(get("/wiki/bestiary/enemies"))
                  .andExpect(status().isOk())
                  .andExpect(view().name("wiki/fragments/enemy-grid :: enemyGrid"));
      }

      @Test
      void shouldReturnEnemyModalFragment() throws Exception {
          Enemy enemy = new Enemy("enemy-anointer", "Anointer", "Desc", "Faithful", 3, 2600, "Melee", "", List.of("Irongate"),
                  75, null, null, 35, null, null, null, null, null, null, "/portrait.png");
          when(enemyRepository.findById("enemy-anointer")).thenReturn(Optional.of(enemy));

          mockMvc.perform(get("/wiki/bestiary/enemy/enemy-anointer"))
                  .andExpect(status().isOk())
                  .andExpect(view().name("wiki/fragments/enemy-modal :: enemyModalContent"))
                  .andExpect(model().attribute("enemy", enemy));
      }

      @Test
      void shouldReturn404ForMissingEnemy() throws Exception {
          when(enemyRepository.findById("missing")).thenReturn(Optional.empty());

          mockMvc.perform(get("/wiki/bestiary/enemy/missing"))
                  .andExpect(status().isNotFound());
      }
  }
  ```

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=BestiaryWikiControllerTest`
  Expected output: Compilation failure (controller does not exist yet)

- [ ] **Step 3: Write minimal implementation**
  - Implement `BestiaryWikiController.java` with `@Controller` and `@RequestMapping("/wiki/bestiary")`.
  - Implement `bestiary.html`, `enemy-grid.html`, and `enemy-modal.html` with:
    - Search input and Gnosis tier pill filters (All, 0, I, II, III, IV, V, VI)
    - Rank pills (Minor, Faithful, Guardian, Boss)
    - Sort dropdown (Gnosis, Health, Name)
    - Enemy cards displaying portrait, Gnosis level badge, health pill, and clean text vulnerability badges (e.g. Fire: Resistant, Shock: Vulnerable)
    - Detail modal with full defense stats and attack notes.

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=BestiaryWikiControllerTest`
  Expected output: PASS

- [ ] **Step 5: Commit**
  `git add src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiController.java src/main/resources/templates/wiki/bestiary.html src/main/resources/templates/wiki/fragments/enemy-*.html src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/BestiaryWikiControllerTest.java && git commit -m "feat(wiki): add Bestiary catalog views and controller endpoints"`

---

### Task 7: CSS Styling Polish & Full Suite Verification

**Files:**
- Modify: `src/main/resources/static/css/main.css` — complete dark-fantasy styling for tarot cards, prophecy warnings, enemy cards, Gnosis chips, and resistance pills without emojis
- Test: All existing and new tests across the entire project

**Interfaces:**
- Produces: Polished dark-fantasy styling integrated seamlessly with existing design.

- [ ] **Step 1: Apply CSS rules**
  Add styles in `main.css`:
  - Tarot proportion card styling (`.arcana-card`, `.arcana-prophecy-tag`, `.arcana-effect-tier`)
  - Prophecy styling (`.prophecy-card`, `.omen-warning-box`, `.omen-effect-text`)
  - Bestiary styling (`.enemy-card`, `.enemy-gnosis-badge`, `.enemy-health-chip`, `.resistance-badge`, `.weakness-badge`)
  - Mobile responsiveness for grid displays.

- [ ] **Step 2: Run full project verification**
  Command: `./mvnw test`
  Expected output: BUILD SUCCESS, 0 failures, 0 errors.

- [ ] **Step 3: Commit**
  `git add src/main/resources/static/css/main.css && git commit -m "style: add dark fantasy CSS for Arcana, Prophecies, and Bestiary components"`
