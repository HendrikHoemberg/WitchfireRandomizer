# Wiki UI Harmonization Implementation Plan

**Goal:** Make the Arcana/Prophecies/Bestiary wiki pages (added in the last 9 commits) use the
same visual language as the rest of the app, and repair the shared components those commits
inadvertently restyled.

**Architecture:** Pure presentation work: remap the new wiki CSS onto the existing design tokens
(`--wf-*` in `main.css` `:root`), delete the parallel hardcoded palette the new sections
introduced, and move the new wiki markup onto the already-established card/tab/modal recipes.
No controller, service, repository, or data changes.

**Tech Stack:** Spring Boot 4 / Java, Thymeleaf, HTMX, Alpine.js, plain CSS
(`src/main/resources/static/css/main.css`), JUnit 5 + MockMvc.

**Spec:** `docs/slimpowers/specs/2026-09-10-arcana-and-bestiary-wiki-design.md`

## Global Constraints

- Do not edit `.mvn/wrapper/*` or generated build artifacts (project `AGENTS.md` — Boundaries).
- Verify (everything): `./mvnw test`; single test: `./mvnw test -Dtest=TestClassName#testMethodName`.
- No new CSS framework, no new web font, no new JS dependency.
- All colors must come from `:root` tokens; every `var(--x)` used must be defined.
- Every changed template must still render under Thymeleaf (no unclosed fragments).

---

## Diagnosis (evidence)

Screenshots: `target/ui-review/shots/` (captured with `target/ui-review/shoot.mjs` against a local
run on `:9091`). Computed-style dump: `target/ui-review/shots/computed-styles.json`.

| # | Root cause | Evidence |
|---|---|---|
| 1 | **Serif fallback on all new headings.** `font-family: var(--font-gothic, serif)` — `--font-gothic` is *never defined*, so it falls back to generic `serif` (Times New Roman). | `/wiki/arcana`: 139 serif vs 2 Arial headings; `/wiki/bestiary`: 62 serif vs 1 Arial; `/` and `/wiki`: 100% Arial. Card titles `serif 17.6px` vs `Arial 24px/700` on `/wiki`. |
| 2 | **A second, different gold.** New code hardcodes `rgba(212, 175, 55, …)` (= `#d4af37`, yellow metallic) 16×, while the app's gold is `--wf-gold: #ddaf7a` (warm parchment). Both appear in the *same* elements (e.g. active tab: `color: var(--wf-gold)` on `background: rgba(212,175,55,.15)`). | `main.css` lines 1243–1245, 1327–1501, 1653, 1703–1715, 1749, 1784. |
| 3 | **Glass replaced by opaque near-black.** `.arcana-card` / `.enemy-card` / `.prophecy-card` / `.detail-modal-box` set `background: #141210`, later in the cascade than `.texture-box`'s `rgba(48,48,48,.44)`, so the translucent texture glass is overridden. | Computed `background-color`: `/wiki` cards `rgb(26,26,26)`, `/wiki/arcana`+`/wiki/bestiary` cards `rgb(20,18,16)`, app boxes `rgba(48,48,48,0.44)`. |
| 4 | **A third tab idiom.** The new `.wiki-subnav-tabs` is a `border-radius:9999px` pill bar with `text-transform:uppercase`, `letter-spacing`, and a gold `box-shadow` glow — the app already has underline tabs (`.exclude-tab-btn`) and flat gray pills (`.filter-pill-btn`); the wiki bar matches neither. | `main.css` 1208–1246 vs `.exclude-tab-btn` (829–856). Visual: `05-wiki-arcana.png` vs `02-randomizer-mid.png`. |
| 5 | **Regression on shared components.** `.detail-modal-box` gained `background #141210`, a gold border, a heavy shadow, and `max-width: 500px → 620px`. That class is shared, so the *pre-existing* randomizer "Item Detail" and "Bead Settings" modals changed too. | `git show 3f623f7:.../main.css` has none of those properties; probe on `/` reports 2 elements with `rgb(20,18,16)` + gold border. |
| 6 | **Box-in-box modal layout.** Every modal block is wrapped in its own outlined sub-box (`.arcana-lore-box`, `.arcana-scaling-card`, `.enemy-overview-box`, `.physical-res-pill`) plus gold uppercase letterspaced `.wiki-modal-section-title`. The app's own modal uses plain sections with a single 1px divider. | `08-wiki-arcana-modal.png` / `11-wiki-enemy-modal.png` vs `03-randomizer-item-modal.png`. |
| 7 | **Hardcoded chrome.** `arcana.html` / `bestiary.html` copy the whole `<head>` and in-line the footer instead of using `~{fragments/footer :: footer}` — and their copy is **missing** "Background image created by Gregory Pedzinski." | `src/main/resources/templates/wiki/arcana.html:195-206`, `bestiary.html:177-188` vs `fragments/footer.html:13`. |
| 8 | **9 px horizontal overflow on every wiki page at 390 px.** `/` = 390/390; `/wiki`, `/wiki/arcana`, `/wiki/bestiary` all report `scrollWidth 399 > clientWidth 390`. Cause: `.wiki-subnav-tabs` is `inline-flex` with no wrapping and the tabs do not shrink. | `probe.mjs` output. On mobile the active label also wraps to two lines (`13-mobile-arcana.png`). |
| 9 | **Stale copy + empty panel.** Bestiary empty state still says "…Gnosis level, or **rank filter**" although the rank filter was deleted in `5dbde60`. The arcana mode switcher keeps a full-width `border-bottom` divider even when the whole filter block is hidden, so the Prophecies view shows a near-empty box with an orphan rule. | `enemy-grid.html:8`; `arcana.html:68`; `07-wiki-prophecies.png`. |
| 10 | **Spacing drift.** `.wiki-search-input { margin-bottom: 1.25rem }` *and* the wrapping `div` has an inline `margin-bottom: 1.25rem` → a 40 px gap where 20 px is intended. | probe `gapSearchToFilterTitle: 40`. |

Not a cause (recorded for completeness): `layout/base.html` is dead code — no controller returns it
and no template references it; all five full pages duplicate their `<head>`. Pre-existing, purely
structural, no visual effect. Left as an optional task.

### Decisions to confirm

- **D1 Typography.** Default: drop the serif, use the app font (`Arial, Helvetica, sans-serif`) and
  the existing title treatment (bold, white) for all card and modal titles. *Alternative:* keep a
  gothic display face, but then add a self-hosted webfont and apply it app-wide — not just to the wiki.
- **D2 Surface.** Default: return the new cards to the translucent `.texture-box` glass already used
  by `/` and `/wiki`. *Alternative:* keep a darker "codex" surface, but then change it app-wide and
  via a token, not per-component.

---

### Task 1: Design-consistency regression tests

**Files:**
- Create: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/ui/WikiDesignConsistencyTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a guard suite that fails on the current tree and passes after Tasks 2–6.

- [ ] **Step 1: Write the failing test**

```java
package dev.hendrikhoemberg.witchfirerandomizer.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WikiDesignConsistencyTest {

    private static final Path MAIN_CSS = Path.of("src/main/resources/static/css/main.css");

    @Autowired
    private MockMvc mockMvc;

    private static String mainCss() throws IOException {
        return Files.readString(MAIN_CSS, StandardCharsets.UTF_8);
    }

    private static Set<String> matches(String css, Pattern pattern) {
        Set<String> found = new HashSet<>();
        Matcher m = pattern.matcher(css);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    @Test
    void shouldDefineEveryCssVariableItUses() throws Exception {
        String css = mainCss();
        Set<String> used = matches(css, Pattern.compile("var\\((--[a-zA-Z0-9-]+)"));
        Set<String> defined = matches(css, Pattern.compile("(--[a-zA-Z0-9-]+)\\s*:"));
        assertThat(used).isSubsetOf(defined);
    }

    @Test
    void shouldUseOnlyTheSingleAppGold() throws Exception {
        String css = mainCss();
        assertThat(css).doesNotContain("212, 175, 55");
        assertThat(css).doesNotContain("#d4af37");
    }

    @Test
    void shouldNotReferenceAnUndefinedGothicFont() throws Exception {
        assertThat(mainCss()).doesNotContain("font-gothic");
    }

    @Test
    void shouldRenderTheSharedFooterCreditOnWikiPages() throws Exception {
        for (String route : new String[]{"/wiki", "/wiki/arcana", "/wiki/bestiary"}) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Gregory Pedzinski")));
        }
    }

    @Test
    void shouldNotMentionTheRemovedRankFilter() throws Exception {
        mockMvc.perform(get("/wiki/bestiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("rank filter"))));
    }
}
```

- [ ] **Step 2: Run the tests — verify they FAIL**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest`
Expected: `shouldDefineEveryCssVariableItUses` FAILS on `--font-gothic`;
`shouldUseOnlyTheSingleAppGold` FAILS (16 occurrences);
`shouldNotReferenceAnUndefinedGothicFont` FAILS (6 occurrences);
`shouldRenderTheSharedFooterCreditOnWikiPages` FAILS on `/wiki/arcana`;
`shouldNotMentionTheRemovedRankFilter` FAILS.

- [ ] **Step 3: Implement** — nothing yet; the implementation is Tasks 2–6.

- [ ] **Step 4: Run the tests — verify they PASS**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: PASS (after Tasks 2–6).

- [ ] **Step 5: Commit**

`git commit -m "test(ui): add wiki design-consistency regression tests"`

---

### Task 2: One gold, one font (tokens + typography)

**Files:**
- Modify: `src/main/resources/static/css/main.css` — `:root` tokens and the wiki block (lines 1205–1975)
- Test: `WikiDesignConsistencyTest` (already written)

**Interfaces:**
- Consumes: nothing.
- Produces: `--wf-gold-rgb: 221, 175, 122;` and `--wf-overlay-dark: rgba(22, 19, 15, 0.7);`, usable as
  `rgba(var(--wf-gold-rgb), 0.35)`.

- [ ] **Step 1: Run the tests — verify they FAIL**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: the three CSS tests FAIL.

- [ ] **Step 2: Implement**

In `:root`, after `--wf-gold-bright`:

```css
  --wf-gold-rgb: 221, 175, 122;
  --wf-overlay-dark: rgba(22, 19, 15, 0.7);
```

Replace **every** `rgba(212, 175, 55, X)` with `rgba(var(--wf-gold-rgb), X)` (keep `X` unchanged),
including `#d4af37` if it appears. Affected selectors: `.wiki-tab-link.active`, `.gnosis-badge`,
`.detail-modal-box`, `.arcana-modal-icon-frame`, `.arcana-scaling-card`,
`.arcana-scaling-header` (two declarations), `.enemy-modal-portrait-frame`, `.variant-badge`.

Delete all six `font-family: var(--font-gothic, serif);` declarations. Titles inherit `--wf-font`
from `body`; do not re-declare a family.

- [ ] **Step 3: Run the tests — verify they PASS**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest#shouldDefineEveryCssVariableItUses+shouldUseOnlyTheSingleAppGold+shouldNotReferenceAnUndefinedGothicFont`
Expected: PASS. `--font-gothic` no longer used, no `212, 175, 55` left.

- [ ] **Step 4: Visual check**

`node target/ui-review/shoot.mjs …` (see Task 7) → arcana/bestiary card titles are now sans-serif and
the gold tint on tabs/badges matches the app's warm gold.

- [ ] **Step 5: Commit**

`git commit -m "refactor(css): unify wiki sections onto the app gold token and app font"`

---

### Task 3: Card surfaces back to the app glass + card recipe

**Files:**
- Modify: `src/main/resources/static/css/main.css` — `.arcana-card`, `.enemy-card`,
  `.prophecy-card`, `.arcana-title-*`, tag/chip colors
- Modify: `src/main/resources/templates/wiki/fragments/arcana-grid.html`,
  `enemy-grid.html`, `prophecy-list.html` — drop per-card inline `style` where a class exists

**Interfaces:**
- Consumes: `.texture-box` (glass) and `.wiki-item-card` / `.wiki-card-header` / `.wiki-card-title`
  recipes from `main.css`.
- Produces: arcana/enemy/prophecy cards that share the `/wiki` equipment card surface.

- [ ] **Step 1: Run the test — verify the surface invariant FAILS**

Add to `WikiDesignConsistencyTest`:

```java
    @Test
    void shouldNotOverrideTheSharedGlassSurfaceOnWikiCards() throws Exception {
        String css = mainCss();
        assertThat(css).doesNotContain("#141210");
        assertThat(css).doesNotContain("#0a0908");
    }
```

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest#shouldNotOverrideTheSharedGlassSurfaceOnWikiCards`
→ expected: FAIL (`#141210` present 4×, `#0a0908` 4×).

- [ ] **Step 2: Implement**

- Delete `background: #141210;` from `.arcana-card`, `.enemy-card`, `.prophecy-card`,
  `.detail-modal-box` — each already carries `.texture-box`, which supplies `--wf-box-bg`.
- Change the icon/portrait boxes from `background: #0a0908` to the existing `.wiki-card-icon-box`
  value `#000`, and reuse `.wiki-card-icon-box` rather than three near-duplicate rules
  (`.arcana-icon-box`, `.prophecy-icon-box`, `.enemy-portrait-box`).
- Move the card titles to the established title treatment in place of the serif gold:

```css
.arcana-card-title,
.enemy-card-title,
.prophecy-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
  line-height: 1.25;
}
```

- Replace the ad-hoc tag/chip browns with tokens:

```css
.arcana-tag,
.rank-badge,
.location-chip {
  font-size: 0.7rem;
  padding: 0.15rem 0.45rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid var(--wf-border-subtle);
  border-radius: 0.25rem;
  color: #d1d5db;
  font-weight: 500;
}
```

- In `arcana-grid.html` / `enemy-grid.html` / `prophecy-list.html`, replace
  `style="cursor: pointer;"` with a `.is-clickable` class defined once:

```css
.is-clickable { cursor: pointer; }
```

- [ ] **Step 3: Run the test — verify it PASSES**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: PASS.

- [ ] **Step 4: Visual check** — cards on `/wiki/arcana` and `/wiki/bestiary` are translucent glass,
  matching `/wiki` equipment cards; the page background artwork reads through them.

- [ ] **Step 5: Commit**

`git commit -m "style(css): return wiki cards to the shared glass surface and title treatment"`

---

### Task 4: Wiki sub-navigation in the app's tab idiom (+ mobile overflow)

**Files:**
- Modify: `src/main/resources/static/css/main.css` — replace `.wiki-subnav-*` / `.wiki-tab-link`
- Modify: `src/main/resources/templates/wiki/fragments/wiki-nav.html`

**Interfaces:**
- Consumes: `.texture-box`, the `.exclude-tabs` / `.exclude-tab-btn` underline-tab recipe.
- Produces: a wiki sub-nav strip whose active tab uses the app's gold underline; no horizontal
  overflow at 390 px.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void shouldNotUseAFullRoundPillBarForWikiSubNavigation() throws Exception {
        assertThat(mainCss()).doesNotContain("border-radius: 9999px;\n  padding: 0.35rem;");
    }
```

Plus, in the same test class, a rendered-markup assertion:

```java
    @Test
    void shouldMarkTheActiveWikiTabAndRenderAllTabs() throws Exception {
        mockMvc.perform(get("/wiki/arcana"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wiki-subnav-tabs")))
                .andExpect(content().string(containsString("wiki-tab-link active")));
    }
```

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest#shouldNotUseAFullRoundPillBarForWikiSubNavigation`
→ expected: FAIL.

- [ ] **Step 2: Implement**

In `main.css`, replace the whole `.wiki-subnav-*` block (currently lines 1205–1246) with:

```css
/* ==========================================================================
   Wiki Sub-Navigation Tabs (matches the app's underline tab idiom)
   ========================================================================== */
.wiki-subnav-container {
  margin-bottom: 1.5rem;
  padding: 0.25rem 0.75rem 0;
}

.wiki-subnav-tabs {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  overflow-x: auto;
  border-bottom: 1px solid var(--wf-border);
}

.wiki-tab-link {
  flex: 0 0 auto;
  padding: 0.5rem 1rem;
  white-space: nowrap;
  color: #d1d5db;
  font-size: 0.95rem;
  font-weight: 500;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: color 0.15s, border-color 0.15s;
}

.wiki-tab-link:hover {
  color: #fff;
}

.wiki-tab-link.active {
  color: #fff;
  border-bottom-color: var(--wf-gold-accent);
}
```

`flex: 0 0 auto` + `white-space: nowrap` + `overflow-x: auto` inside a bordered strip removes the
overflow and keeps every label on one line.

In `wiki-nav.html`, give the strip the shared box surface and keep the fragment contract unchanged:

```html
<div th:fragment="wikiNav(activeSection)" class="wiki-subnav-container">
    <nav class="wiki-subnav-tabs" aria-label="Wiki navigation">
        <a href="/wiki" class="wiki-tab-link" th:classappend="${activeSection == 'equipment' ? 'active' : ''}">Equipment</a>
        <a href="/wiki/arcana" class="wiki-tab-link" th:classappend="${activeSection == 'arcana' ? 'active' : ''}">Arcana &amp; Prophecies</a>
        <a href="/wiki/bestiary" class="wiki-tab-link" th:classappend="${activeSection == 'bestiary' ? 'active' : ''}">Bestiary</a>
    </nav>
</div>
```

- [ ] **Step 3: Run the tests — verify they PASS**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest+WikiNavigationTest` → expected: PASS.

- [ ] **Step 4: Verify no mobile overflow**

Re-run `probe.mjs` at 390 px for `/wiki`, `/wiki/arcana`, `/wiki/bestiary`;
expected: `scrollWidth === clientWidth === 390` for all three (currently 399).

- [ ] **Step 5: Commit**

`git commit -m "style(ui): move wiki sub-nav to the app tab idiom and fix mobile overflow"`

---

### Task 5: Modal harmonization + revert the shared-modal regression

**Files:**
- Modify: `src/main/resources/static/css/main.css` — `.detail-modal-box`, `.wiki-modal-*`
- Modify: `src/main/resources/templates/wiki/fragments/arcana-modal.html`, `enemy-modal.html`
- Modify: `src/main/resources/templates/wiki/arcana.html`, `bestiary.html` — modal width class

**Interfaces:**
- Consumes: `.texture-box`, `.wiki-desc-section` / `.wiki-stats-grid` from the item card.
- Produces: one modal surface shared by the randomizer and the wiki; wiki modals are wider by an
  explicit modifier only.

- [ ] **Step 1: Run the test — verify it FAILS**

```java
    @Test
    void shouldNotRestyleTheSharedModalBoxForWikiOnly() throws Exception {
        String css = mainCss();
        // The shared modal box keeps the pre-overhaul geometry and no own surface.
        assertThat(css).doesNotContain("max-width: 620px;");
    }
```

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest#shouldNotRestyleTheSharedModalBoxForWikiOnly`
→ expected: FAIL.

- [ ] **Step 2: Implement**

Restore `.detail-modal-box` to the pre-overhaul rule (surface comes from `.texture-box`):

```css
.detail-modal-box {
  max-width: 500px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
}

.detail-modal-box.wiki-modal-box {
  max-width: 620px;
}
```

and add `wiki-modal-box` next to `detail-modal-box texture-box` in `arcana.html:176` and
`bestiary.html:158`.

Flatten the modal content — one surface, one divider per section, no nested boxes:

```css
.wiki-modal-container { padding: 1.5rem; }

.wiki-modal-header {
  display: flex;
  gap: 1.25rem;
  align-items: flex-start;
  padding-bottom: 1.25rem;
  padding-right: 2.25rem;
  border-bottom: 1px solid var(--wf-border);
}

.wiki-modal-section {
  padding: 1rem 0;
  border-bottom: 1px solid var(--wf-border);
  font-size: 0.95rem;
  color: #d1d5db;
}

.wiki-modal-section:last-child { border-bottom: none; }

.wiki-modal-section-title {
  font-size: 1rem;
  font-weight: 500;
  color: #fff;
  margin: 0 0 0.5rem 0;
}
```

Replace `.arcana-lore-box` and `.enemy-overview-box` with a single `.wiki-note-box`:

```css
.wiki-note-box {
  border-left: 2px solid var(--wf-border);
  padding-left: 1rem;
  font-size: 0.9rem;
  color: #d1d5db;
}
```

In `arcana-modal.html` / `enemy-modal.html`, swap `arcana-lore-box` / `enemy-overview-box` for
`wiki-note-box` (fragment names and model attributes unchanged), and drop
`.arcana-scaling-card`'s gradient/inset-shadow wrapper in favour of the flat `.wiki-modal-section`
with the effect line styled as:

```css
.arcana-scaling-effect {
  font-size: 1rem;
  font-weight: 600;
  color: var(--wf-gold-bright);
}
```

- [ ] **Step 3: Run the tests — verify they PASS**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest+ArcanaWikiControllerTest+BestiaryWikiControllerTest`
→ expected: PASS.

- [ ] **Step 4: Visual check**

Screenshot `/wiki/arcana?modal=arcana-accelerant`, `/wiki/bestiary?modal=enemy-anointer`, and the
randomizer's item modal (`/` → click an "i" button). All three must read as the same surface with
the same 500 px randomizer / 620 px wiki geometry.

- [ ] **Step 5: Commit**

`git commit -m "fix(ui): revert shared modal regression and flatten wiki modal content"`

---

### Task 6: Template hygiene (shared footer, spacing, empty panel, stale copy)

**Files:**
- Modify: `src/main/resources/templates/wiki/arcana.html`, `bestiary.html`
- Modify: `src/main/resources/templates/wiki/fragments/enemy-grid.html`
- Modify: `src/main/resources/static/css/main.css` — `.wiki-search-input`
- Test: `WikiDesignConsistencyTest`

**Interfaces:**
- Consumes: `~{fragments/footer :: footer}`.
- Produces: wiki pages with the same footer markup as every other page; no double search margin.

- [ ] **Step 1: Run the tests — verify they FAIL**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest#shouldRenderTheSharedFooterCreditOnWikiPages+shouldNotMentionTheRemovedRankFilter`
→ expected: both FAIL on the current tree.

- [ ] **Step 2: Implement**

- In `arcana.html` (lines 195–206) and `bestiary.html` (lines 177–188), replace the inlined
  `<footer class="site-footer">…</footer>` block with:
  `<div th:replace="~{fragments/footer :: footer}"></div>`
- In `enemy-grid.html:8`, change the copy to
  `Try adjusting your search criteria or Gnosis level.`
- In `main.css`, remove `margin-bottom: 1.25rem;` from `.wiki-search-input` (the wrapper already
  supplies it) so the gap is 20 px, not 40 px.
- In `arcana.html`, wrap the mode switcher + filters in a container that is hidden as a whole in
  Prophecies view, so no orphan divider is left behind:

```html
<div x-show="viewMode === 'cards'">
  <!-- mode switcher (with its border-bottom) + search + filters -->
</div>
```

  keeping the mode switcher itself always visible above it (the switcher buttons stay outside the
  `x-show` container; only the divider and the filter block are inside).

- In `bestiary.html`, add the results header used on `/wiki` so the three catalog pages behave alike:

```html
<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
  <div style="font-size: 0.95rem; color: #ffffff;">
    Showing <span th:text="${#lists.size(enemies)}">0</span>
    <span th:text="${#lists.size(enemies) == 1 ? 'enemy' : 'enemies'}">enemies</span>
  </div>
</div>
```

- [ ] **Step 3: Run the tests — verify they PASS**

Run: `./mvnw test -Dtest=WikiDesignConsistencyTest` → expected: PASS.

- [ ] **Step 4: Commit**

`git commit -m "refactor(wiki): share the footer fragment and clean up wiki page chrome"`

---

### Task 7: Full verification

- [ ] **Step 1: Whole suite**

Run: `./mvnw test` → expected: BUILD SUCCESS, no failures.

- [ ] **Step 2: Re-capture the visual evidence**

```bash
cd target/ui-review
nohup chromium --headless=new --no-sandbox --disable-gpu --hide-scrollbars \
  --remote-debugging-port=9333 --user-data-dir="$PWD/chrome-profile" \
  --window-size=1440,1000 about:blank > chrome.log 2>&1 &
sleep 3
node shoot.mjs 9333 http://127.0.0.1:9091 "$PWD/shots-after" "$PWD/spec.json"
node probe.mjs 9333 http://127.0.0.1:9091 "/,/wiki,/wiki/arcana,/wiki/bestiary" "$PWD/probe.js"
```

Expected: `01` vs `05` / `09` share one font, one gold, one card surface; every route reports
`overflows: false` at 390 px; `headingFonts` on `/wiki/arcana` contains no `serif`.

- [ ] **Step 3: Compare before/after screenshots side by side** and confirm each of the ten
  diagnosis points is addressed or explicitly waived.

- [ ] **Step 4: Commit** any remaining changes and report the before/after evidence.

---

## Self-review

**Spec coverage:** the design spec (`2026-09-10-arcana-and-bestiary-wiki-design.md`) asked for a
"dark fantasy aesthetic" for the new sections. Tasks 2–5 deliver that *inside* the app's existing
token system instead of a parallel one; the spec's functional requirements (routes, fragments,
filtering) are untouched.

**Placeholder scan:** no TBD/TODO; every step carries the code or the exact command.

**Type consistency:** the fragment signatures (`wikiNav(activeSection)`, `arcanaGrid`,
`enemyGrid`, `prophecyList`, `arcanaModalContent`, `enemyModalContent`) and the controller view
names in Task 5/6 are unchanged, so `ArcanaWikiControllerTest`, `BestiaryWikiControllerTest` and
`WikiNavigationTest` remain valid.

**Open decisions:** D1 (typography) and D2 (surface) are the only taste calls; both have a
recommended default and an alternative stated above. Everything else is mechanical.
