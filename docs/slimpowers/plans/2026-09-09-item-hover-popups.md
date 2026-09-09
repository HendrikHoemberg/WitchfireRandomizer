# Item Hover Popups & Spawning Optimization Implementation Plan

**Goal:** Reintroduce and optimize item hover popups across Loadout slots, Bead slots, and the Exclude Items list with robust viewport-aware floating positioning.

**Architecture:** Server-rendered popup content templates via a shared Thymeleaf fragment (`item-popup.html`) coupled with a single top-level Alpine.js floating container (`#hover-item-popup`). Alpine.js dynamically injects the hovered item template, measures true element bounding boxes via `$nextTick()`, and clamps coordinates within viewport boundaries without hardcoded height approximations or container clipping.

**Tech Stack:** Spring Boot 4.1.1, Java 25, Thymeleaf, Alpine.js 3.14.8, HTML5/CSS3, JUnit 5, MockMvc.

**Spec:** Hover popup behavior from `WitchfireLoadoutManager` (`ItemCardPopup.tsx`, `BeadCardPopup.tsx`), redesigned to resolve positioning bugs and container clipping.

## Global Constraints
- Preserve slot locking, item modal opening (`openItemModal`), and HTMX rerolling.
- No hardcoded height approximations (such as 500px or 600px). The positioning engine must measure the actual rendered DOM box.
- Floating popup must have `pointer-events: none` to prevent cursor entrapment and jittery enter/leave hover flickers.
- Verification command: `./mvnw test`.

---

### Task 1: Shared Item Popup Fragment

**Files:**
- Create: `src/main/resources/templates/randomizer/fragments/item-popup.html` (Unified Thymeleaf fragment rendering header with icon, name, category, element pill/dot; weapon stats; melee stats; bead requirements & description; and Mysterium levels).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Interfaces:**
- Consumes: `item` (`Item` or subclass `Weapon`, `MeleeWeapon`, `Bead`, `Spell`, `MagicalItem`)
- Produces: HTML markup for `.item-card-popup-inner` to be embedded inside `<template class="popup-template">`

- [x] **Step 1: Write test checking popup fragment elements rendered on index page**
- [x] **Step 2: Run test — verify failure**
  `./mvnw test -Dtest=RandomizerControllerTest#testItemPopupTemplatesRendered`
- [x] **Step 3: Create `src/main/resources/templates/randomizer/fragments/item-popup.html`**
- [x] **Step 4: Run single test — verify PASS**
  `./mvnw test -Dtest=RandomizerControllerTest#testItemPopupTemplatesRendered`

---

### Task 2: Floating Popup Portal & Optimized Positioning Logic

**Files:**
- Modify: `src/main/resources/static/css/main.css` (Style `#hover-item-popup.floating-popup` with `position: fixed`, `z-index: 1000`, `pointer-events: none`, `max-height: calc(100vh - 24px)`, `overflow-y: auto`, clean drop shadow).
- Modify: `src/main/resources/templates/randomizer/index.html` (Add `hoveredItemHtml`, `popupStyle`, `showPopup(el)`, `hidePopup()`, `positionPopup(el)` to Alpine `x-data`, and add the floating popup element at the container level).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Interfaces:**
- Consumes: Target element `el` passed on `@mouseenter`
- Produces: Dynamic measurement and placement of `#hover-item-popup` clamped within viewport margins (`margin = 12px`, `gap = 8px`), flipping left when right edge overflows.

- [x] **Step 1: Add unit test verifying the floating popup container exists in the HTML**
- [x] **Step 2: Run test — verify failure**
- [x] **Step 3: Implement CSS in `main.css` and Alpine methods in `randomizer/index.html`**
- [x] **Step 4: Run test — verify PASS**

---

### Task 3: Integrate Popups into Slots, Beads, and Exclude Items

**Files:**
- Modify: `src/main/resources/templates/randomizer/fragments/slot-card.html` (Replace old clipped popup with `<template class="popup-template">`, bind `@mouseenter="showPopup($el)"` and `@mouseleave="hidePopup()"`).
- Modify: `src/main/resources/templates/randomizer/fragments/bead-slots.html` (Replace old clipped popup with `<template class="popup-template">`, bind `@mouseenter="showPopup($el)"` and `@mouseleave="hidePopup()"`).
- Modify: `src/main/resources/templates/randomizer/index.html` (Add `<template class="popup-template">` inside `.exclude-card`, bind `@mouseenter="showPopup($el)"` and `@mouseleave="hidePopup()"`).
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Interfaces:**
- Consumes: `itemPopup` fragment from Task 1
- Produces: Working hover popups on all loadout slots, bead slots, and exclusion list cards.

- [x] **Step 1: Write test verifying popup templates are present in slots and exclude cards**
- [x] **Step 2: Run test — verify failure**
- [x] **Step 3: Update `slot-card.html`, `bead-slots.html`, and `randomizer/index.html`**
- [x] **Step 4: Run single test — verify PASS**

---

### Task 4: Verification & Regression Testing

**Files:**
- Test: Entire test suite

- [x] **Step 1: Run full verification `./mvnw test`**
- [x] **Step 2: Verify all 6+ test classes pass with zero errors**
