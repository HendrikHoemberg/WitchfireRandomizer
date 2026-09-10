# Witchfire Wiki Expansion: Arcana, Prophecies & Bestiary - Architecture & Design Specification

- **Date**: 2026-09-10
- **Status**: Draft for Review
- **Author**: Antigravity & Hendrik

---

## 1. Context & Goals

The Witchfire Companion application currently provides an equipment **Item Wiki** (`/wiki`) and a **Loadout Randomizer** (`/`, `/randomizer`). 

To make the wiki a comprehensive encyclopedia for Witchfire players, this expansion incorporates two major in-game systems:
1. **Arcana & Prophecies Compendium (`/wiki/arcana`)**:
   - Covers all 121 in-run **Arcana** cards and 18 **Prophecies** (and their associated **Omens**).
   - Allows searching, filtering by element/category, inspecting card tier scaling, and viewing which Arcana pool each Prophecy unlocks alongside its corresponding Omen curse/affix.
2. **Bestiary & Vulnerability Guide (`/wiki/bestiary`)**:
   - Covers all 62 enemy types in Witchfire.
   - Highlights enemy ranks (Minor, Faithful, Guardian/Boss), Gnosis requirements (0–6), spawn locations, health pools, and visual elemental weakness/resistance indicators (Fire, Shock, Freeze, Decay).
3. **Unified Wiki Hub Navigation**:
   - Seamless sub-navigation tabs across all wiki views (`Equipment`, `Arcana & Prophecies`, `Bestiary`), unified with the existing dark-fantasy theme and sidebar drawer.

### Design Directive
- **Strictly no emojis**: All tabs, badges, cards, and buttons must use clean typography, styled CSS pill badges, and subtle SVG icons that seamlessly integrate into Witchfire's dark-fantasy UI.

### Non-Goals
- Full combat simulator or damage calculator (keep the app lightweight, responsive, and server-rendered).
- Account creation or server-side user databases (all data remains bundled locally).

---

## 2. Information Architecture & Navigation

### 2.1 Route Structure
```
GET /wiki                      -> Wiki Hub (Equipment & Items catalog, default view)
GET /wiki/items                -> HTMX fragment: Filtered items grid
GET /wiki/item/{id}            -> HTMX fragment: Equipment item modal

GET /wiki/arcana               -> Arcana & Prophecies page
GET /wiki/arcana/cards         -> HTMX fragment: Filtered Arcana card grid
GET /wiki/arcana/card/{id}     -> HTMX fragment: Arcana card detail modal
GET /wiki/arcana/prophecies    -> HTMX fragment: Prophecies & Omens catalog

GET /wiki/bestiary             -> Bestiary page
GET /wiki/bestiary/enemies     -> HTMX fragment: Filtered Enemy card grid
GET /wiki/bestiary/enemy/{id}  -> HTMX fragment: Enemy detail modal
```

### 2.2 Navigation UI
- **Wiki Sub-Navigation Bar**: A responsive tab bar positioned beneath the header on all `/wiki*` routes:
  - `Equipment` (`/wiki`)
  - `Arcana & Prophecies` (`/wiki/arcana`)
  - `Bestiary` (`/wiki/bestiary`)
- **Sidebar Drawer**: Expanded to list:
  - Loadout Randomizer (`/`)
  - Equipment Wiki (`/wiki`)
  - Arcana & Prophecies (`/wiki/arcana`)
  - Bestiary (`/wiki/bestiary`)

---

## 3. Data Pipeline & Storage

### 3.1 MediaWiki Cargo Ingestion (`scripts/scrape_wiki.py`)
Extend the Python scraper to query the MediaWiki Cargo tables on `witchfire.wiki.gg`:
1. `Arcana`: `_pageName`, `name`, `prophecyTypes`, `description`, `effects`.
2. `Prophecies`: `_pageName`, `name`, `description`, `omenName`, `arcanaType`, `location`.
3. `Omens`: `_pageName`, `name`, `effect` (joined with Prophecies).
4. `Enemy`: `_pageName`, `name`, `description`, `rank`, `gnosis`, `health`, `damage`, `variants`, `location`, `fireResistance`, `earthResistance`, `waterResistance`, `airResistance`, `burnResistance`, `decayResistance`, `freezeResistance`, `shockResistance`, `stunResistance`, `staggerResistance`.

Outputs:
- `src/main/resources/data/arcana.json`
- `src/main/resources/data/prophecies.json`
- `src/main/resources/data/enemies.json`
- `src/main/resources/static/images/arcana/<id>.png`
- `src/main/resources/static/images/prophecies/<id>.png`
- `src/main/resources/static/images/enemies/<id>.png`

Graceful fallback: When an enemy or card image is missing from the wiki, assign a clean category placeholder SVG / CSS badge.

---

## 4. Backend Architecture

### 4.1 Domain Models
- **`Arcana`**:
  - `id`: String slug (e.g. `arcana-accelerant`)
  - `name`: String
  - `description`: String
  - `effects`: String (e.g. `Damage to burning enemies: +15% / +25%`)
  - `prophecyTypes`: List<String> (e.g. `["Fire Element"]`, `["Bull", "Firearms"]`)
  - `element`: Element enum (Fire, Water, Air, Earth, or null/Neutral)
  - `iconUrl`: String
- **`Prophecy`**:
  - `id`: String slug (e.g. `prophecy-of-air`)
  - `name`: String
  - `description`: String
  - `arcanaType`: String (matches Arcana `prophecyTypes`)
  - `omenName`: String
  - `omenEffect`: String
  - `location`: String
  - `iconUrl`: String
  - `associatedArcanaCount`: int
- **`Enemy`**:
  - `id`: String slug (e.g. `enemy-anointer`)
  - `name`: String
  - `description`: String
  - `rank`: String (`Minor`, `Faithful`, `Guardian`, `Boss`, etc.)
  - `gnosis`: Integer (0–6)
  - `health`: Integer
  - `damage`: String
  - `variants`: String
  - `locations`: List<String>
  - `fireResistance`, `earthResistance`, `waterResistance`, `airResistance`: Integer
  - `burnResistance`, `decayResistance`, `freezeResistance`, `shockResistance`: Integer
  - `stunResistance`, `staggerResistance`: Integer
  - `iconUrl`: String
  - Convenience helper methods: `isWeakTo(Element)`, `isResistantTo(Element)`.

### 4.2 Repositories
- **`ArcanaRepository`** (`@Component`):
  - Loads `arcana.json` and `prophecies.json` via Jackson on `@PostConstruct`.
  - Search & filter: by keyword (name & description), element, prophecy type.
  - Find by ID, and query Arcana belonging to a specific Prophecy.
- **`EnemyRepository`** (`@Component`):
  - Loads `enemies.json` via Jackson on `@PostConstruct`.
  - Search & filter: by keyword, Gnosis level (or minimum/maximum), rank, location, and elemental weakness.
  - Sorting: by Gnosis ascending/descending, Health, or Name.

### 4.3 Controllers
- **`WikiController`**:
  - Retains existing `/wiki` and `/wiki/items` routes.
  - Adds `/wiki/arcana`, `/wiki/arcana/cards`, `/wiki/arcana/card/{id}`, `/wiki/arcana/prophecies`.
  - Adds `/wiki/bestiary`, `/wiki/bestiary/enemies`, `/wiki/bestiary/enemy/{id}`.

---

## 5. Frontend & UI Design

### 5.1 Design Language
Adheres to the established dark-fantasy aesthetic:
- Background: `#0e0d0b`, card texture: `#161412` with subtle borders (`#2d2922`).
- Accent: Witchfire embers and golds (`#d4af37`, `#e5c158`, `#c2410c`).
- Element Badges: Fire (`#ff4d4d`), Water (`#4da6ff`), Earth (`#66cc66`), Air (`#ffcc00`).

### 5.2 Arcana & Prophecy View (`/wiki/arcana`)
- **Top Filter Bar**:
  - Search input (live debounced with HTMX).
  - Mode switch: "Arcana Cards" vs "Prophecies & Omens".
  - Filter pills: All, Fire, Water, Air, Earth, Firearms, Spells, Survival.
- **Arcana Grid**:
  - Tarot-proportioned card frames with luminous element runes.
  - Displays name, primary prophecy type tag, concise description, and tier 1/2 effects.
  - Clicking card opens an item-style modal.
- **Prophecies & Omens Grid**:
  - Prophecy card displaying unlock map, Arcana pool boosted, and highlighted **Omen Curse** badge in dark crimson.
  - Clicking a Prophecy triggers an inline filter showing only the Arcana cards granted by it.

### 5.3 Bestiary View (`/wiki/bestiary`)
- **Top Filter Bar**:
  - Search input.
  - Gnosis pills: All, Gnosis 0, I, II, III, IV, V, VI.
  - Rank pills: All, Minor, Faithful, Guardian, Boss.
  - Sort dropdown: Gnosis (Low -> High), Health (High -> Low), Name (A-Z).
- **Enemy Grid**:
  - Enemy portrait with rank frame.
  - Gnosis level badge (e.g. `GNOSIS III`).
  - Base health display.
  - **Elemental Vulnerability / Resistance Badges**:
    - Clean text/icon badges: "Weak", "Normal", "Resistant" or numeric percentages.
    - Colored badges for Fire (#ff4d4d), Shock (#ffcc00), Freeze (#4da6ff), Decay (#66cc66).
- **Enemy Modal**:
  - High-res portrait and lore background.
  - Spawn locations list.
  - Complete defense table (all resistance values, stun/stagger thresholds).

---

## 6. Testing & Quality Assurance

1. **Unit & Repository Tests**:
   - `ArcanaRepositoryTest`: Test JSON loading, search by keyword, filter by element/prophecy, prophecy-arcana linkage.
   - `EnemyRepositoryTest`: Test JSON loading, Gnosis filtering, rank filtering, sorting, resistance calculation.
2. **Controller Tests (`@WebMvcTest`)**:
   - Verify `/wiki/arcana` renders full view with sub-navigation active.
   - Verify `/wiki/arcana/cards` returns card grid HTMX fragment.
   - Verify `/wiki/bestiary` renders full view.
   - Verify `/wiki/bestiary/enemies` returns enemy grid HTMX fragment.
   - Verify 404 response on non-existent card or enemy ID.
3. **Full Project Verification**:
   - Execute `./mvnw test` before completion.
