# Faithful UI Rewrite Implementation Plan

**Goal:** Faithfully replicate the UI, styling, components, and user experience of the original Witchfire Randomizer site (https://www.witchfire-randomizer.com/) using Spring Boot, Thymeleaf, HTMX, and Alpine.js, while retaining Melee Weapons as a new equipment category and loadout slot.

**Architecture:** Server-side rendered views via Thymeleaf with HTMX endpoints for loadout generation, item filtering, and modal detail rendering, backed by Alpine.js for client-side local UI state (slot locking, bead settings modal, hover popups, mobile drawer nav, collapsible Mysterium tiers, client-side filtering/sorting).

**Tech Stack:** Spring Boot 4.1.0, Java 26, Thymeleaf, HTMX 2.0.4, Alpine.js 3.14.8, Jackson 3.x, JUnit 5, MockMvc.

**Spec:** Original Next.js source code in `/home/hendrik/Documents/Coding/WitchfireLoadoutManager/` and live site `https://www.witchfire-randomizer.com/`.

## Global Constraints
- Only the Item Wiki and Loadout Randomizer are in scope (no Savefile Editor or Loadout Manager).
- Melee Weapons must remain a first-class equipment slot and wiki category.
- Port configured to 9090 (`server.port=9090`).
- Custom CSS must replicate the original colors (`#30303071`, `#1a1a1a`, `#818181`, `#505050`, `#ddaf7aa6`, `#ffcb8a`, red locked state `#b91c1c` / `border-red-900`) without external Tailwind build steps.
- Verification command: `./mvnw test`.

---

### Task 1: Navigation Drawer & Header Alignment

**Files:**
- Modify: `src/main/resources/templates/fragments/navigation.html` (Ensure hamburger button, slide-in drawer with "Loadout Randomizer" and "Item Wiki" links only, centered Witchfire logo).
- Modify: `src/main/resources/templates/layout.html` (Ensure centered layout structure and texture overlay).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiControllerTest.java` (Verify header and navigation render correctly).

**Interfaces:**
- Consumes: Standard layout template.
- Produces: Drawer navigation matching original `Navigation.tsx`.

- [ ] **Step 1: Write test checking navigation and header markup**
- [ ] **Step 2: Run test — verify it passes / validates current state**
- [ ] **Step 3: Update `navigation.html` and `layout.html` to match exact header dimensions, drawer styling, and texture background**
- [ ] **Step 4: Run `./mvnw test` to verify passing**

---

### Task 2: Faithful Randomizer Page & Loadout Display

**Files:**
- Modify: `src/main/resources/templates/randomizer/index.html` (Full layout matching `app/randomizer/page.tsx`: Current Loadout card, Generate New Loadout button, Beads card, Stat Requirements table, Element Preferences card, Exclude Items card, Bead Settings modal).
- Modify: `src/main/resources/templates/randomizer/fragments/loadout-grid.html` (Square `w-30 h-30` slots for Primary, Secondary, Demonic, Melee, Relic, Fetish, Ring, Light Spell, Heavy Spell, plus element pills).
- Modify: `src/main/resources/templates/randomizer/fragments/slot-card.html` (Detailed slot rendering: empty state `+`, populated state with black square icon, item name, element dot, "i" info button, lock border/text, hover tooltip, and modal overlay).
- Modify: `src/main/resources/templates/randomizer/fragments/bead-slots.html` (Bead slots 1–5, locking, requirements summary).
- Modify: `src/main/resources/static/css/main.css` (Exact slot dimensions `120px`-`136px`, borders, shadows, glow-pulse, parchment texture).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Interfaces:**
- Consumes: `RandomizerService.randomize(RandomizerRequest) -> Loadout`
- Produces: Exact interactive loadout interface with slot locking, element preferences, bead stat requirements, and empty slot mode.

- [ ] **Step 1: Write MockMvc tests for randomizer endpoints (generate with locks, empty slots, element prefs)**
- [ ] **Step 2: Run test — verify failure / pass**
- [ ] **Step 3: Implement Thymeleaf fragments with Alpine.js state for locking, popup tooltips, and bead stats**
- [ ] **Step 4: Run `./mvnw test` to verify passing**

---

### Task 3: Item Detail Modal & Popups (Loadout & Beads)

**Files:**
- Create: `src/main/resources/templates/fragments/item-modal.html` (Item and Bead popup & modal component matching `ItemCardPopup.tsx` and `BeadCardPopup.tsx`).
- Modify: `src/main/resources/templates/randomizer/index.html` (Include modal container triggered by "i" buttons).
- Modify: `src/main/resources/templates/wiki/index.html` (Support direct linking / scrollTo).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Interfaces:**
- Consumes: `Item` or `Bead`
- Produces: Modal with full Mysterium tiers / requirements and "Show item in wiki" button linking to `/wiki?search=...`.

- [ ] **Step 1: Write test for modal rendering fragment**
- [ ] **Step 2: Run test — verify failure**
- [ ] **Step 3: Implement item and bead modal fragments with full stats and Mysterium details**
- [ ] **Step 4: Run `./mvnw test` to verify passing**

---

### Task 4: Faithful Wiki Page & Wide Item Cards

**Files:**
- Modify: `src/main/resources/templates/wiki/index.html` (Search input with clear button, "Filter by Category" pills, "Filter by Element" pills, results count, "Sort By" select dropdown, category groupings).
- Modify: `src/main/resources/templates/wiki/fragments/item-card.html` (Full-width wide cards matching `ItemCard.tsx` and `BeadCard.tsx`: dark header with icon & element, stats grid for weapons, description, collapsible Mysterium tiers with expand/collapse all).
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiController.java` (Support category, element, query, and sort criteria).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiControllerTest.java`

**Interfaces:**
- Consumes: Filter params (`category`, `element`, `q`, `sort`)
- Produces: Full-width responsive wiki item list matching original layout.

- [ ] **Step 1: Write tests for wiki filtering, category grouping, and sorting**
- [ ] **Step 2: Run tests — verify failure/pass**
- [ ] **Step 3: Implement wide card layout, collapsible Mysterium levels, and bead cards**
- [ ] **Step 4: Run `./mvnw test` to verify passing**

---

### Task 5: Visual Verification & Real-User Workflow Comparison

**Files:**
- Test/Verification: Run headless Chrome to take screenshots of the running app on `http://localhost:9090` (Randomizer default state, locked state, item modal, bead settings, wiki default state, wiki category filter, wiki search).
- Compare with live site screenshots (`/tmp/wf_screenshots/live_randomizer.png`, `/tmp/wf_screenshots/live_wiki.png`).
- Full test suite verification (`./mvnw test`).

- [ ] **Step 1: Take screenshots of both pages and interactions on localhost:9090**
- [ ] **Step 2: View and compare side-by-side with live screenshots**
- [ ] **Step 3: Run `./mvnw test` (non-negotiable verification-before-completion)**
