# Request Logging and Host File Rotation Implementation Plan

**Goal:** Implement lightweight HTTP request logging (ignoring static assets), startup data loading logs, and rolling file logging configured for `/var/log/witchfire` via Docker volume mounts.

**Architecture:** A Spring `OncePerRequestFilter` intercepts requests, records response time and real client IP, and outputs formatted HTTP logs while bypassing static asset paths (`/images/**`, `/css/**`, `/favicon.ico`). Spring Boot's Logback rolling policy is configured to output to a configurable directory (`LOG_PATH`), which `docker-compose.yml` mounts from `/var/log/witchfire` on the host to `/app/logs` in the container.

**Tech Stack:** Java 25, Spring Boot 4.1.1, JUnit 5, MockMvc, Docker / Docker Compose.

**Spec:** In-session requirements: request logging for non-static requests, rotating physical files in host directory `/var/log/witchfire`, safe container permissions.

## Global Constraints

- Java 25 source/target compatibility.
- Never log static assets (`/images/**`, `/css/**`, `/favicon.ico`, `/robots.txt`).
- Respect reverse-proxy forwarded headers (`X-Forwarded-For`) for client IP reporting.
- Maintain existing test coverage and pass all tests via `./mvnw test`.
- Provide concrete host setup instructions for directory permissions.

---

### Task 1: RequestLoggingFilter and Unit Tests

**Files:**
- Create: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/config/RequestLoggingFilter.java`
- Test: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/config/RequestLoggingFilterTest.java`

**Interfaces:**
- Consumes: `jakarta.servlet.http.HttpServletRequest`, `jakarta.servlet.http.HttpServletResponse`, `jakarta.servlet.FilterChain`
- Produces: Log output for dynamic routes (e.g. `INFO [Request] GET /wiki?category=WEAPON 200 OK (12ms) [203.0.113.4]`) at `INFO` (2xx/3xx), `WARN` (4xx), `ERROR` (5xx); skips static resource URIs.

- [ ] **Step 1: Write the failing test**
  Create `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/config/RequestLoggingFilterTest.java` testing:
  1. Requests to dynamic endpoints (`/`, `/wiki`, `/randomizer/reroll`) invoke logger with method, URI, status, duration, and remote IP.
  2. Static asset requests (`/css/main.css`, `/images/items/b-acute.png`, `/favicon.ico`) bypass request logging.
  3. Query strings are captured when present (`/wiki?search=fire`).

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=RequestLoggingFilterTest` -> Expected: Compilation failure (class does not exist yet).

- [ ] **Step 3: Write minimal implementation**
  Create `RequestLoggingFilter.java`:
  - Extend `OncePerRequestFilter`.
  - Check `shouldNotFilter(HttpServletRequest request)` for prefix `/images/`, `/css/`, `/favicon.ico`, `/robots.txt`.
  - Capture start time (`System.currentTimeMillis()`), call `filterChain.doFilter(request, response)`.
  - Calculate elapsed time, extract `request.getMethod()`, `request.getRequestURI()`, query string, `response.getStatus()`, and `request.getRemoteAddr()`.
  - Log with formatted message at appropriate level (`INFO` for status < 400, `WARN` for 400-499, `ERROR` for 500+).

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=RequestLoggingFilterTest` -> Expected: Tests pass.

- [ ] **Step 5: Commit**
  `git commit -m "feat: add RequestLoggingFilter for non-static HTTP traffic"`

---

### Task 2: Dataset Loading and Startup Summary Logging

**Files:**
- Modify: `src/main/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ItemRepository.java`
- Modify: `src/test/java/dev/hendrikhoemberg/witchfirerandomizer/repository/ItemRepositoryTest.java`

**Interfaces:**
- Consumes: Item dataset loaded from `/data/items.json`
- Produces: `INFO` log on startup detailing total items loaded broken down by category.

- [ ] **Step 1: Write the failing test**
  Add test in `ItemRepositoryTest.java` asserting that logging is performed during `init()`.

- [ ] **Step 2: Run the single test — verify it FAILS**
  Command: `./mvnw test -Dtest=ItemRepositoryTest` -> Expected: FAIL (verify missing log statement).

- [ ] **Step 3: Write minimal implementation**
  In `ItemRepository.java`:
  - Add SLF4J `Logger logger = LoggerFactory.getLogger(ItemRepository.class)`.
  - In `init()`: After successfully populating `items` and `itemsById`, count items by category and log an `INFO` summary:
    `"Successfully loaded {} items from /data/items.json ({} weapons, {} spells, {} magical items, {} beads)"`.

- [ ] **Step 4: Run the single test — verify it PASSES**
  Command: `./mvnw test -Dtest=ItemRepositoryTest` -> Expected: Tests pass.

- [ ] **Step 5: Commit**
  `git commit -m "feat: add dataset initialization logging in ItemRepository"`

---

### Task 3: Rolling File Logback Configuration and Gitignore

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-prod.properties`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: Environment variable `LOG_PATH` (defaults to `logs`)
- Produces: Rolling log files `${LOG_PATH}/witchfire.log` with daily & 10MB rollover, gzipped archives, retaining 14 days and up to 100MB max.

- [ ] **Step 1: Update .gitignore**
  Add `logs/` and `*.log` to `.gitignore` so local development log files are not committed to git.

- [ ] **Step 2: Configure logging in application.properties and application-prod.properties**
  Configure Spring Boot logging properties:
  - `logging.file.name=${LOG_PATH:logs}/witchfire.log`
  - `logging.logback.rollingpolicy.file-name-pattern=${LOG_PATH:logs}/witchfire-%d{yyyy-MM-dd}.%i.log.gz`
  - `logging.logback.rollingpolicy.max-file-size=10MB`
  - `logging.logback.rollingpolicy.max-history=14`
  - `logging.logback.rollingpolicy.total-size-cap=100MB`
  - `logging.level.dev.hendrikhoemberg.witchfirerandomizer=INFO`
  - `logging.level.org.springframework.web=WARN`

- [ ] **Step 3: Run all tests to verify configuration validity**
  Command: `./mvnw test` -> Expected: BUILD SUCCESS (Spring context starts without logback property conflicts).

- [ ] **Step 4: Commit**
  `git commit -m "feat: configure Logback rolling file logging with LOG_PATH"`

---

### Task 4: Docker, Docker Compose, and VPS Runbook Updates

**Files:**
- Modify: `Dockerfile`
- Modify: `docker-compose.yml`
- Modify: `docs/vps-deployment-checklist.md`

**Interfaces:**
- Consumes: Host directory `/var/log/witchfire`
- Produces: Volume mount to `/app/logs` inside container with consistent UID/GID (1000:1000) for unprivileged writing.

- [ ] **Step 1: Update Dockerfile**
  - Explicitly set UID/GID 1000 for `spring` user:
    `RUN groupadd -g 1000 spring && useradd -u 1000 -g spring -m spring`
  - Create directory `/app/logs` with `chown -R spring:spring /app/logs`.

- [ ] **Step 2: Update docker-compose.yml**
  - Set `LOG_PATH: /app/logs` in `environment`.
  - Add volume mount under `volumes:`:
    `- /var/log/witchfire:/app/logs`

- [ ] **Step 3: Update docs/vps-deployment-checklist.md**
  Add host preparation instructions:
  ```bash
  sudo mkdir -p /var/log/witchfire
  sudo chown -R 1000:1000 /var/log/witchfire
  sudo chmod 750 /var/log/witchfire
  ```
  Add log monitoring command:
  ```bash
  # View live application request and error logs on host:
  tail -f /var/log/witchfire/witchfire.log
  ```

- [ ] **Step 4: Verify full project build and tests**
  Command: `./mvnw clean package` -> Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**
  `git commit -m "chore: configure Docker volume for /var/log/witchfire and update deployment runbook"`
