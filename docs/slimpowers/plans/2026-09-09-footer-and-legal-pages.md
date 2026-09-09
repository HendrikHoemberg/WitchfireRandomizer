# Footer & Privacy Policy / Impressum Implementation Plan

**Goal:** Migrate the original footer from `WitchfireLoadoutManager` to `WitchfireRandomizer`, including the legal disclaimers, asset credits, and new dedicated Privacy Policy and Impressum routes.

**Architecture:** A Spring Boot MVC controller (`LegalController`) serving static Thymeleaf views for `/privacy` and `/impressum`, styled with the project's dark texture box theme. A shared Thymeleaf fragment (`fragments/footer.html`) included across all pages (`randomizer/index.html`, `wiki/index.html`, `layout/base.html`, error pages, and legal pages) with sticky footer styling in `main.css`.

**Tech Stack:** Java 25, Spring Boot 4, Thymeleaf, JUnit 5 / MockMvc, vanilla CSS.

**Spec:** User request to implement Privacy Policy / Impressum links and migrate the original footer from `/home/hendrik/Documents/Coding/WitchfireLoadoutManager/src/app/layout.tsx`.

## Global Constraints
- Zero external CSS/JS build dependencies (vanilla CSS, HTMX, Alpine.js).
- Dark gothic theme matching existing `.texture-box` / CSS variables (`--wf-bg`, `--wf-gold`, etc.).
- Preserves all existing copyright notices, credits (The Astronauts, Gregory Pedzinski), and 100% test coverage.

---

### Task 1: LegalController and Legal Pages (`/privacy` and `/impressum`)

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalController.java`
- Create: `src/main/resources/templates/legal/privacy.html`
- Create: `src/main/resources/templates/legal/impressum.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalControllerTest.java`

**Interfaces:**
- Consumes: HTTP GET `/privacy`, HTTP GET `/impressum`
- Produces: Rendered views `legal/privacy` and `legal/impressum` containing the privacy statement (no tracking, no cookies, local storage only, standard server logs) and impressum (provider info, copyright notices, fan project disclaimer).

- [x] **Step 1: Write the failing test**
Create `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalControllerTest.java`:
```java
package dev.hendrikhoemberg.witchfirerandomizer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LegalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPrivacyPolicyPageRenders() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/privacy"))
                .andExpect(content().string(containsString("Privacy Policy")))
                .andExpect(content().string(containsString("No Cookies & No Tracking")))
                .andExpect(content().string(containsString("Local Storage")));
    }

    @Test
    void testImpressumPageRenders() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/impressum"))
                .andExpect(content().string(containsString("Impressum")))
                .andExpect(content().string(containsString("Legal Notice")))
                .andExpect(content().string(containsString("The Astronauts")));
    }
}
```

- [x] **Step 2: Run the single test — verify it FAILS**
Run:
```bash
./mvnw test -Dtest=LegalControllerTest
```
Expected: FAIL (404 Not Found or compiler error because LegalController does not exist).

- [x] **Step 3: Write minimal implementation**
Create `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalController.java`:
```java
package dev.hendrikhoemberg.witchfirerandomizer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("pageTitle", "Privacy Policy");
        model.addAttribute("activeTab", "privacy");
        return "legal/privacy";
    }

    @GetMapping("/impressum")
    public String impressum(Model model) {
        model.addAttribute("pageTitle", "Impressum / Legal Notice");
        model.addAttribute("activeTab", "impressum");
        return "legal/impressum";
    }
}
```

Create `src/main/resources/templates/legal/privacy.html` and `src/main/resources/templates/legal/impressum.html` with navigation and styled `.texture-box` containers.

- [x] **Step 4: Run the single test — verify it PASSES**
Run:
```bash
./mvnw test -Dtest=LegalControllerTest
```
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalController.java src/main/resources/templates/legal/ src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalControllerTest.java
git commit -m "feat(legal): add privacy policy and impressum routes and templates"
```

---

### Task 2: Shared Footer Fragment & Sticky Footer CSS

**Files:**
- Create: `src/main/resources/templates/fragments/footer.html`
- Modify: `src/main/resources/static/css/main.css`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalControllerTest.java`

**Interfaces:**
- Consumes: Thymeleaf fragment call `th:replace="~{fragments/footer :: footer}"`
- Produces: Consistent footer with copyright notice, Astronauts/Pedzinski disclaimer, and Privacy Policy / Impressum links.

- [x] **Step 1: Write the failing test**
Add footer assertions to `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalControllerTest.java`:
```java
    @Test
    void testLegalPagesIncludeFooter() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("site-footer")))
                .andExpect(content().string(containsString("Witchfire Randomizer 1.0.0")))
                .andExpect(content().string(containsString("Gregory Pedzinski")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("href=\"/impressum\"")));
    }
```

- [x] **Step 2: Run the single test — verify it FAILS**
Run:
```bash
./mvnw test -Dtest=LegalControllerTest#testLegalPagesIncludeFooter
```
Expected: FAIL (missing `site-footer` / footer links).

- [x] **Step 3: Write minimal implementation**
Create `src/main/resources/templates/fragments/footer.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<footer th:fragment="footer" class="site-footer">
    <div class="footer-content">
        <div class="footer-links">
            <a href="/privacy">Privacy Policy</a>
            <span class="footer-separator">•</span>
            <a href="/impressum">Impressum</a>
        </div>
        <p class="footer-copy">&copy; Witchfire Randomizer 1.0.0</p>
        <p class="footer-disclaimer">I am not affiliated with The Astronauts or Witchfire in any way.</p>
        <p class="footer-credits">All image rights belong to Witchfire and The Astronauts. Background image created by Gregory Pedzinski.</p>
    </div>
</footer>
</body>
</html>
```

Add footer styles and sticky footer rules to `src/main/resources/static/css/main.css`:
```css
/* Sticky footer flex layout */
body {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

main.container {
  flex: 1 0 auto;
}

.site-footer {
  background-color: rgba(26, 26, 26, 0.733);
  border-top: 1px solid var(--wf-border-subtle);
  color: var(--wf-text-muted);
  padding: 1.25rem 1rem;
  margin-top: 3rem;
  width: 100%;
  text-align: center;
  font-size: 0.75rem;
  line-height: 1.6;
}

.footer-content {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}

.footer-links a {
  color: var(--wf-gold);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;
}

.footer-links a:hover {
  color: var(--wf-gold-bright);
  text-decoration: underline;
}

.footer-separator {
  color: var(--wf-border);
}
```

Include `th:replace="~{fragments/footer :: footer}"` in `legal/privacy.html` and `legal/impressum.html`.

- [x] **Step 4: Run the single test — verify it PASSES**
Run:
```bash
./mvnw test -Dtest=LegalControllerTest#testLegalPagesIncludeFooter
```
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add src/main/resources/templates/fragments/footer.html src/main/resources/static/css/main.css src/main/resources/templates/legal/ src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/LegalControllerTest.java
git commit -m "feat(footer): create site footer fragment with legal links and sticky layout"
```

---

### Task 3: Integrate Footer into Main App Pages & Full Verification

**Files:**
- Modify: `src/main/resources/templates/randomizer/index.html`
- Modify: `src/main/resources/templates/wiki/index.html`
- Modify: `src/main/resources/templates/layout/base.html`
- Modify: `src/main/resources/templates/error/404.html`
- Modify: `src/main/resources/templates/error/error.html`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiControllerTest.java`

**Interfaces:**
- Consumes: Existing views
- Produces: Every page on the site renders the unified footer with legal links.

- [x] **Step 1: Write the failing tests**
In `RandomizerControllerTest.java`:
```java
    @Test
    void testRandomizerRendersFooter() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("site-footer")))
                .andExpect(content().string(containsString("Witchfire Randomizer 1.0.0")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("href=\"/impressum\"")));
    }
```
In `WikiControllerTest.java`:
```java
    @Test
    void testWikiRendersFooter() throws Exception {
        mockMvc.perform(get("/wiki"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("site-footer")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("href=\"/impressum\"")));
    }
```

- [x] **Step 2: Run the tests — verify they FAIL**
Run:
```bash
./mvnw test -Dtest=RandomizerControllerTest#testRandomizerRendersFooter,WikiControllerTest#testWikiRendersFooter
```
Expected: FAIL (footer not yet in `randomizer/index.html` or `wiki/index.html`).

- [x] **Step 3: Write minimal implementation**
Add `<div th:replace="~{fragments/footer :: footer}"></div>` right before `</body>` in:
- `src/main/resources/templates/randomizer/index.html`
- `src/main/resources/templates/wiki/index.html`
- `src/main/resources/templates/layout/base.html`
- `src/main/resources/templates/error/404.html`
- `src/main/resources/templates/error/error.html`

- [x] **Step 4: Run the single tests — verify they PASS**
Run:
```bash
./mvnw test -Dtest=RandomizerControllerTest#testRandomizerRendersFooter,WikiControllerTest#testWikiRendersFooter
```
Expected: PASS.

- [x] **Step 5: Run full project verification**
Run:
```bash
./mvnw test
```
Expected: All tests pass with 0 failures, 0 errors.

- [x] **Step 6: Commit**
```bash
git add src/main/resources/templates/ src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/
git commit -m "feat(footer): integrate footer across all pages and add verification tests"
```
