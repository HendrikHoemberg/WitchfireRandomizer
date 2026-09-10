# Bestiary Portrait Dossier & WebP Asset Migration

**Goal:** Make the Bestiary card content as good as the other three wiki pages — by giving the
enemy art the space it deserves and replacing the resistance pills with a readable ledger — and
cut the image payload the site ships.

**Architecture:** A new `Affinity` record plus an `AffinityType` enum turn the ten raw
`*Resistance` integers into ordered, coloured, render-ready rows; `Enemy` exposes
`getResistances()` / `getVulnerabilities()`. The card becomes a two-column grid (portrait plate +
dossier). A Python optimizer re-encodes every shipped asset to WebP, and a JUnit test pins the
budgets so a future scrape cannot re-inflate them.

**Tech Stack:** Spring Boot 4.1 / Java 25, Thymeleaf, hand-written `main.css`, Python + Pillow for
the asset pipeline, JUnit 5 / MockMvc / AssertJ for tests.

**Spec:** No spec doc — driven by rendered mockups iterated with the user
(`target/ui-review/mockup.html`, `mockup-affinity.html`, `mockup-color.html`,
`mockup-color2.html`).

## Global Constraints

- Verify command: `./mvnw test`.
- One gold only (`--wf-gold` family); card surfaces come from the shared `.texture-box` glass.
- Every CSS variable used must be defined in `:root`.
- Red is reserved for "danger to you" (reset actions, imposed omens) — not for enemy weaknesses.
- Filenames, formats and `iconUrl` values must stay consistent: the test
  `WikiDesignConsistencyTest#shouldOnlyReferenceImageFilesThatExist` renders every page and
  asserts each referenced `/images/...` file exists on disk.

---

## Why the cards looked bad (measured)

1. **The art was drawn at ~48 px.** Enemy renders are 1024x1024 with the figure filling ~80% of
   the canvas (mean ink 84% x 96%); in the old 3.5 rem portrait box minus padding the figure
   landed at 47.5 x 41.6 px. Arcana gets away with a 52 px icon because those are flat white
   glyphs — these are photoreal, so they read as mud, and dark ones vanished into the black box.
2. **The wide card was mostly empty.** Territory content filled only 33–46% of the 614 px card
   and the full-width stat slab put `ATTACK` at the 50% mark with the right half of both cells
   empty.
3. **Pills made the data the smallest thing on the card** and put resistances and vulnerabilities
   in the same visual channel.

## Decisions (user-approved)

| Decision | Choice |
|---|---|
| Card composition | Portrait-led dossier: 168 px portrait plate + dossier column (2-column grid kept) |
| Stats display | **Ledger**: `name · severity bar · value`, adaptive 2-up entry grid |
| Resist colour | **Bone/ivory `#cbc3b2`** — achromatic, so it never collides with an element dot |
| Vulnerability colour | **Ember `#e8834f`** — red stays reserved for danger |
| Element identity | 6 px muted dot per row (`AffinityType` colours) |
| Ordering | Vulnerabilities **first** (the actionable info), then resistances |
| Magnitude encoding | Bar *length* (clamped at 100%) **and** fill *opacity* (55–100%) |
| Asset format | Convert all shipped art to **WebP q82** |

---

## Task 1: Affinity model

**Files:** Create `model/Affinity.java`, `model/AffinityType.java`; modify `model/Enemy.java`;
test `model/AffinityTest.java`.

- [x] Wrote `AffinityTest` first — 7 tests covering the resistance/vulnerability split, zero and
      null handling, the `-200%` clamp, intensity scaling, display formatting, and that every
      `AffinityType` carries a display name and hex colour.
- [x] Watched it fail (compile error: `getResistances()` / `Affinity` missing) — verified RED.
- [x] Implemented. `Affinity` is a record with `vulnerable()`, `strength()`,
      `intensityPercent()`, `displayValue()`. `AffinityType.valueFor(Enemy)` is a single switch;
      declaration order is the canonical display order.
- [x] GREEN: 7/7.
- [x] Fixed one wrong assertion of mine (`value()` is the raw signed value, so `-200`, not `200`).

## Task 2: Card fragment and CSS

**Files:** Modify `templates/wiki/fragments/enemy-grid.html`, `static/css/main.css`,
`templates/wiki/bestiary.html`; test `ui/WikiDesignConsistencyTest.java`.

- [x] Wrote 6 new design tests first (ledger not pills, vulnerabilities first, magnitude in the
      bar, element dots, portrait plate in a grid, dedicated colour tokens) — verified RED with
      exactly those 6 failing.
- [x] Rebuilt the fragment: the 20 hardcoded affinity spans collapse into two `th:each` loops over
      `enemy.vulnerabilities` / `enemy.resistances`.
- [x] Added `--wf-resist`, `--wf-resist-bright`, `--wf-vulnerable`, `--wf-vulnerable-bright` to
      `:root`; removed the dead `.affinity-pill*`, `.enemy-combat-stats-row`,
      `.enemy-affinity-strip`, `.enemy-affinity-block`, `.enemy-affinity-row`,
      `.enemy-affinity-pills`, `.enemy-card-body` and `.enemy-portrait-box` rules.
- [x] GREEN: 29/29 in the class, 110/110 overall.

## Task 3: WebP asset migration

**Files:** Create `scripts/optimize_images.py`, `assets/ImageAssetBudgetTest.java`;
modify the four `data/*.json`, `scripts/scrape_wiki.py`, `static/css/main.css`,
7 templates + 3 tests for the logo path.

- [x] Wrote `ImageAssetBudgetTest` first — verified RED against the 1024 px / 568 KB originals.
- [x] Wrote the optimizer. It resizes to a per-group maximum edge, encodes WebP q82, deletes the
      superseded file, and skips assets that are already WebP and already small enough
      (so re-runs are no-ops and never re-encode).
- [x] **Ran the numbers before choosing the format:** PNG at 512 px still costs 245 KB/enemy
      (17 MB total) versus 41 KB as WebP — 5.9x. PNG could not reach the target payload, so the
      format was changed with the user's approval.
- [x] Converted 340 assets: **70.5 MB -> 6.4 MB**.
- [x] Rewrote all 337 `iconUrl` values to `.webp`, wired the optimizer into `scrape_wiki.py` as
      its final step, and normalised the two mixed-case `.webP` references.
- [x] Updated the budget test to WebP budgets from the measured maxima plus headroom.

### Verification of the migration

- Measured on the real page over CDP resource timing after scrolling the whole roster:
  **65 image requests, 3.0 MB, all WebP, zero broken images** (was ~36 MB of enemy PNGs).
- Programmatic check of all 62 rendered cards: 47 render a ledger, 15 render the explicit
  `No elemental resistances or weaknesses.` state, 0 render both, 0 render neither.

## Bugs the process caught (worth remembering)

1. **Stale browser CSS** made the first layout verification show a half-applied design. Fixed by
   always starting Chromium with a cold profile; verified with a computed-style probe.
2. **Stale Java classes**: `spring.thymeleaf.cache=false` reloads *templates* but not *classes*, so
   the already-running app on :9090 served a 500 once the template called the new getters. Verify
   against a freshly started instance (the project's running server needs a restart after any Java
   change).
3. **`optimize_images.py` lowercased `.webP` -> `.webp`**, breaking `/images/wf-logo2.webP` on every
   page. No existing test fetched an asset, so it slipped through until the browser payload probe
   showed broken images. Added `shouldOnlyReferenceImageFilesThatExist` and proved it fails by
   hiding a referenced image.
4. **A `max-width: 560px` override placed before the base rule** lost on source order and broke the
   mobile card. Moved after `.enemy-card`/`.enemy-card-portrait`.
