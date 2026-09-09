# VPS Security Hardening & Caddy Integration Implementation Plan

**Goal:** Harden the Witchfire Randomizer application for VPS deployment, addressing all findings from the comprehensive security audit including default network binding, bounded query parameter handling, defense-in-depth security headers, reverse proxy rate limiting, and systemd sandbox isolation.

**Architecture:** Spring Boot 4.1.1 web application running Java 25 bound strictly to localhost (`127.0.0.1:9090`) across all profiles, defended in-depth by an application-layer `SecurityHeadersFilter` and fronted by a Caddy reverse proxy with automated TLS, security headers, request limits, rate limiting, and systemd process isolation.

**Tech Stack:** Java 25, Spring Boot 4.1.1, WebMVC, Thymeleaf, Caddy, Systemd, JUnit 5 / MockMvc.

**Spec:** Comprehensive Security Audit Report (2026-09-09).

## Global Constraints

- Java 25 compatibility (`<java.version>25</java.version>`).
- Must run `./mvnw test` and maintain a 100% green test suite.
- TDD required for all production code changes (`NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST`).
- Caddy acts as the public TLS termination point; Spring Boot must never listen on `0.0.0.0`.

---

## Audit Findings & Risk Summary

| Finding ID | Severity | Description | Mitigation Strategy |
|---|---|---|---|
| **SEC-01** | Medium | Default profile lacks `server.address` binding, risking `0.0.0.0` exposure if run without `-Dspring.profiles.active=prod`. | Add `server.address=127.0.0.1` directly to base `application.properties`. |
| **SEC-02** | Medium | Absence of rate limiting on server-side rendering endpoints (`/randomizer/reroll`, `/wiki/items`). | Configure Caddy rate limiting directives / fail2ban integration and document UFW firewall rules. |
| **SEC-03** | Low | Application relies 100% on Caddy for HTTP security headers; direct access lacks headers. | Implement `SecurityHeadersFilter` in Spring Boot for defense-in-depth. |
| **SEC-04** | Low | CSP requires `'unsafe-eval'` for Alpine.js reactive directive parsing. | Document framework constraint in Caddyfile and deployment plan. |
| **SEC-05** | Low | Unbounded string splitting on `beads` query parameter in `RandomizerController`. | Bound parsing to a maximum of 5 beads in `restoreLoadoutFromParams`. |

---

### Task 1: Error Handling for Missing Item IDs

**Files:**
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiController.java`
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/WikiControllerTest.java`
- Create: `src/main/resources/templates/error/404.html`

**Interfaces:**
- Consumes: HTTP `GET /wiki/item/{id}` with unknown `id`.
- Produces: HTTP 404 (Not Found) status code instead of unhandled 500 `NoSuchElementException`.

- [x] **Step 1: Write the failing test**
  Add `testWikiItemModalReturnsNotFoundForUnknownId()` in `WikiControllerTest.java`:
  ```java
  @Test
  void testWikiItemModalReturnsNotFoundForUnknownId() throws Exception {
      mockMvc.perform(get("/wiki/item/non-existent-id"))
              .andExpect(status().isNotFound());
  }
  ```

- [x] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=WikiControllerTest#testWikiItemModalReturnsNotFoundForUnknownId`
  Expected: FAIL with `NoSuchElementException` / 500 status.

- [x] **Step 3: Write minimal implementation**
  In `WikiController.java`, catch or map missing item to `ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")`:
  ```java
  @GetMapping("/item/{id}")
  public String getItemModal(@PathVariable String id, Model model) {
      Item item = itemRepository.findById(id)
              .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                      org.springframework.http.HttpStatus.NOT_FOUND, "Item not found: " + id));
      model.addAttribute("item", item);
      return "wiki/fragments/item-modal :: itemModalContent";
  }
  ```
  Create `src/main/resources/templates/error/404.html` providing a clean, non-disclosing error view for browser navigation.

- [x] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=WikiControllerTest#testWikiItemModalReturnsNotFoundForUnknownId`
  Expected: PASS.

---

### Task 2: Production Configuration Hardening

**Files:**
- Create: `src/main/resources/application-prod.properties`
- Modify: `src/main/resources/templates/error/error.html`

**Interfaces:**
- Produces: Production profile overrides: localhost binding, reverse proxy forward header awareness, template cache enabled, and suppressed stack trace disclosures.

- [x] **Step 1: Create application-prod.properties**
  Configured `server.address=127.0.0.1`, `server.forward-headers-strategy=framework`, `server.error.include-stacktrace=never`, `server.error.whitelabel.enabled=false`, `spring.thymeleaf.cache=true`.

- [x] **Step 2: Verify test suite passes**
  Command: `./mvnw test`
  Expected: PASS.

---

### Task 3: Caddyfile & Systemd Production Deployment Templates

**Files:**
- Create: `deploy/Caddyfile`
- Create: `deploy/witchfire.service`

**Interfaces:**
- Produces: Caddy reverse proxy configuration with TLS, request body limits, access logging, and HTTP security headers. Systemd unit with strict Linux sandbox primitives.

- [x] **Step 1: Create deploy/Caddyfile**
  Configured `reverse_proxy 127.0.0.1:9090`, security headers, body size limit `1MB`, access logs.

- [x] **Step 2: Create deploy/witchfire.service**
  Configured `User=witchfire`, `ProtectSystem=strict`, `ProtectHome=true`, `PrivateTmp=true`, `NoNewPrivileges=true`, `SPRING_PROFILES_ACTIVE=prod`.

---

### Task 4: Base Configuration Localhost Binding Hardening (Fix SEC-01)

**Files:**
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Produces: Safe default network binding to `127.0.0.1` so that even if the app is launched without `SPRING_PROFILES_ACTIVE=prod`, it never listens on `0.0.0.0`.

- [x] **Step 1: Add server.address=127.0.0.1 to application.properties**
  Update `src/main/resources/application.properties`:
  ```properties
  spring.application.name=witchfirerandomizer
  server.address=127.0.0.1
  server.port=9090

  # Development default: disable template caching so changes reflect immediately on page refresh
  spring.thymeleaf.cache=false
  ```

- [x] **Step 2: Run test suite to verify configuration validity**
  Command: `./mvnw test`
  Expected: PASS.

---

### Task 5: Bounded Bead Collection Query Parameter Parsing (Fix SEC-05)

**Files:**
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerController.java`
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/controller/RandomizerControllerTest.java`

**Interfaces:**
- Consumes: `GET /randomizer?beads=id1,id2,id3,id4,id5,id6,id7...`
- Produces: Cap bead collection parsing to at most 5 beads (the game loadout capacity), ignoring surplus IDs.

- [x] **Step 1: Write the failing test**
  Add `testRandomizerCapsBeadsParameterToMaximumFive()` and `testRandomizerRestoresLoadoutWhenOnlyBeadsParamProvided()` in `RandomizerControllerTest.java`.

- [x] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=RandomizerControllerTest#testRandomizerCapsBeadsParameterToMaximumFive`
  Expected: FAIL (assertion expected size 5 but was 7).

- [x] **Step 3: Write minimal implementation**
  In `RandomizerController.java` inside `restoreLoadoutFromParams`:
  ```java
  if (beadsParam != null && !beadsParam.isBlank()) {
      List<Bead> beads = new ArrayList<>();
      String[] parts = beadsParam.split(",");
      for (int i = 0; i < parts.length && beads.size() < 5; i++) {
          itemRepository.findById(parts[i].trim())
                  .filter(item -> item instanceof Bead)
                  .ifPresent(item -> beads.add((Bead) item));
      }
      loadout.setBeads(beads);
  }
  ```

- [x] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=RandomizerControllerTest#testRandomizerCapsBeadsParameterToMaximumFive`
  Expected: PASS.

---

### Task 6: Application-Layer Defense-in-Depth Security Headers Filter (Fix SEC-03)

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/config/SecurityHeadersFilter.java`
- Create: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/config/SecurityHeadersFilterTest.java`

**Interfaces:**
- Consumes: All incoming HTTP servlet requests.
- Produces: Injects baseline security headers on every response:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`

- [x] **Step 1: Write the failing test**
  Create `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/config/SecurityHeadersFilterTest.java`.

- [x] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=SecurityHeadersFilterTest#testBaselineSecurityHeadersArePresentOnResponse`
  Expected: FAIL (Response header 'X-Content-Type-Options' expected:<nosniff> but was:<null>).

- [x] **Step 3: Write minimal implementation**
  Create `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/config/SecurityHeadersFilter.java` implementing `jakarta.servlet.Filter`.

- [x] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=SecurityHeadersFilterTest#testBaselineSecurityHeadersArePresentOnResponse`
  Expected: PASS.

---

### Task 7: Reverse Proxy Rate Limiting & DoS Hardening (Fix SEC-02 & SEC-04)

**Files:**
- Modify: `deploy/Caddyfile`

**Interfaces:**
- Produces: Production-ready Caddy configuration with rate limiting documentation, `caddy-ratelimit` directives, request timeouts, and CSP explanation for Alpine.js.

- [x] **Step 1: Enhance deploy/Caddyfile**
  Added rate limiting configuration block (for `caddy-ratelimit` module) and fail2ban setup guidance. Documented CSP `'unsafe-eval'` rationale.

---

### Task 8: Full Verification & VPS Hardening Checklist

- [x] **Step 1: Run full verification suite**
  Command: `./mvnw test`
  Output: 39 tests run, 0 failures, 0 errors, 0 skipped. BUILD SUCCESS.

- [x] **Step 2: VPS Host Hardening Checklist**
  1. **Firewall (UFW)**:
     ```bash
     sudo ufw default deny incoming
     sudo ufw default allow outgoing
     sudo ufw allow 22/tcp
     sudo ufw allow 80/tcp
     sudo ufw allow 443/tcp
     sudo ufw enable
     ```
  2. **Dedicated User & File Permissions**:
     ```bash
     sudo useradd -r -s /bin/false witchfire
     sudo mkdir -p /opt/witchfirerandomizer/logs
     sudo chown -R witchfire:witchfire /opt/witchfirerandomizer/logs
     sudo chmod 750 /opt/witchfirerandomizer/logs
     sudo chown root:root /opt/witchfirerandomizer/witchfirerandomizer.jar
     sudo chmod 644 /opt/witchfirerandomizer/witchfirerandomizer.jar
     ```
  3. **Systemd Service**:
     ```bash
     sudo cp deploy/witchfire.service /etc/systemd/system/
     sudo systemctl daemon-reload
     sudo systemctl enable --now witchfire.service
     ```
  4. **Caddy Reverse Proxy**:
     ```bash
     sudo cp deploy/Caddyfile /etc/caddy/Caddyfile
     sudo systemctl reload caddy
     ```
