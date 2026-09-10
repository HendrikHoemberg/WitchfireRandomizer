# Witchfire Randomizer

A companion web app for the game [Witchfire](https://witchfire.wiki.gg): a **Loadout Randomizer** and an **Item Wiki**, served as a server-rendered Spring Boot application.

The app focuses exclusively on:

1. **Loadout Randomizer** — generate randomized loadouts with slot locks, element preferences, item exclusions, bead stat constraints, and shareable links.
2. **Item Wiki** — a filterable, searchable encyclopedia of Witchfire equipment with detailed stats and expandable Mysteria tiers.

---

## Features

### Loadout Randomizer (`/`, `/randomizer`)
- Generates a full loadout across all equipment slots: Primary Weapon, Secondary Weapon, Demonic Weapon, Melee Weapon, Light Spell, Heavy Spell, Relic, Fetish, Ring, and up to 5 Beads.
- **Slot locks** — lock any slot to keep it fixed while rerolling the rest.
- **Single-slot reroll** — reroll one slot without touching the others.
- **Element preferences** — prefer Fire / Water / Air / Earth when filling slots (with graceful fallback when no matching item exists).
- **Item exclusions** — exclude specific items from the pool by category, with search.
- **Bead constraints** — set character stats (Flesh, Blood, Mind, Witchery, Arsenal, Faith) and available bead slots; only eligible beads are drawn.
- **Empty Slot Mode** — allow non-primary slots to randomly come up empty.
- **Shareable links** — encode the current loadout into a URL and copy it to the clipboard.
- **Hover popups & detail modals** — inspect items inline without leaving the page.

### Item Wiki (`/wiki`)
- Full catalog across Weapons, Demonic Weapons, Melee Weapons, Light/Heavy Spells, Relics, Fetishes, Rings, and Beads.
- Search by name/description, filter by category and element (including "No Element"), and sort by category, name, or element.
- Detail modals with stats, lore, and expandable Mysteria tiers.

### Platform
- Baseline security headers on every response and a request logging filter (method, URI, status, duration, client IP; static assets skipped).
- Rolling file logging with size/retention caps.
- Production profile tuned for running behind a reverse proxy.

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| Runtime | Java 25 |
| Build | Apache Maven (`./mvnw` wrapper) |
| Backend | Spring Boot 4.1 (`spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf`) |
| Templating | Thymeleaf templates & fragments |
| Interactivity | [HTMX](https://htmx.org/) (server-driven DOM swaps), [Alpine.js](https://alpinejs.dev/) (modals, local state, clipboard) |
| Styling | Hand-written CSS (dark-fantasy Witchfire aesthetic), no Node build step |
| Data | JSON dataset loaded from the classpath at startup |
| Tests | JUnit 5, MockMvc / `@SpringBootTest` |

---

## Architecture

```
src/main/resources/data/items.json  +  static/images/items/*.png
        │
        ▼
ItemRepository (loads & indexes items at startup)
        │
        ├── RandomizerService ──► RandomizerController ──► Thymeleaf + HTMX/Alpine
        └── search/grouping ─────► WikiController ────────► Thymeleaf + HTMX/Alpine
```

- **`ItemRepository`** (`repository/`) reads `/data/items.json` on `@PostConstruct`, builds an id index, and exposes category/search/eligibility queries.
- **`RandomizerService`** (`service/`) applies slot resolution rules (locking, exclusions, element preference, primary≠secondary, bead eligibility).
- **Controllers** (`controller/`) render full pages and HTMX fragments; the frontend swaps fragments instead of reloading.
- **Models** (`model/`) use Jackson polymorphic typing on `category`, so `Weapon`, `MeleeWeapon`, `Spell`, `MagicalItem`, and `Bead` all deserialize from the same JSON array.

---

## Getting Started

### Prerequisites
- **JDK 25** (the Maven wrapper downloads Maven itself)

### Run locally
```bash
./mvnw spring-boot:run
```

The app starts on **http://127.0.0.1:9090** by default.

### Build a runnable JAR
```bash
./mvnw clean package
java -jar target/witchfirerandomizer-0.0.1-SNAPSHOT.jar
```

### Tests
```bash
# Everything
./mvnw test

# A single test class / method
./mvnw test -Dtest=RandomizerServiceTest
./mvnw test -Dtest=RandomizerServiceTest#someMethod
```

---

## Configuration

Defaults live in `src/main/resources/application.properties`; production overrides in `application-prod.properties` (activate with `SPRING_PROFILES_ACTIVE=prod`).

| Property / Env var | Default | Description |
| --- | --- | --- |
| `server.address` | `127.0.0.1` | Bind address (localhost-only; put a reverse proxy in front) |
| `server.port` | `9090` | HTTP port |
| `LOG_PATH` | `logs` | Directory for `witchfire.log` and rolled archives |
| `SPRING_PROFILES_ACTIVE` | — | Set to `prod` for production settings |

Logging uses a rolling policy: 10 MB max file size, 14-day history, 100 MB total cap.

In production, stack traces are suppressed, the whitelabel error page is disabled, Thymeleaf caching is enabled, and `X-Forwarded-*` headers are honored.

---

## Data

All item data is bundled with the application:

- `src/main/resources/data/items.json` — the full item dataset (currently 136 items).
- `src/main/resources/static/images/items/` — item icons referenced by each entry.

`ItemRepository` loads and indexes the dataset at startup, so no external service or database is required.

---

## Project Structure

```
src/main/java/dev/hendrikhoemberg/witchfirerandomizer/
├── config/        # RequestLoggingFilter, SecurityHeadersFilter
├── controller/    # RandomizerController, WikiController, LegalController
├── model/         # Item hierarchy, Loadout, RandomizerRequest, enums
├── repository/    # ItemRepository (JSON-backed)
└── service/       # RandomizerService (slot resolution)

src/main/resources/
├── data/items.json          # Bundled item dataset
├── static/                  # css/, js/ (htmx, alpine), images/
└── templates/               # Thymeleaf pages + HTMX fragments

src/test/java/...            # Unit, repository, service, and MockMvc tests
docs/                        # Design specification
```

---

## HTTP Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/`, `/randomizer` | Randomizer page; accepts `primary`, `secondary`, `demonic`, `melee`, `light`, `heavy`, `relic`, `fetish`, `ring`, `beads` query params to restore a shared loadout |
| `POST` | `/randomizer/reroll` | Regenerate the loadout; returns the `loadout-grid` HTMX fragment |
| `POST` | `/randomizer/clear` | Clear all slots; returns the `loadout-grid` fragment |
| `POST` | `/randomizer/reroll-slot` | Reroll a single slot; returns the `slot-card` fragment |
| `GET` | `/wiki` | Wiki catalog page |
| `GET` | `/wiki/items` | Filtered/sorted item grid fragment (`category`, `element`, `noElement`, `search`, `sort`) |
| `GET` | `/wiki/item/{id}` | Item detail modal fragment |
| `GET` | `/privacy` | Privacy policy |
| `GET` | `/impressum` | Legal notice |

---

## Security Notes

- `SecurityHeadersFilter` sets `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, and `Permissions-Policy` on all responses.
- The production profile binds to localhost and expects a reverse proxy to terminate TLS.

---

## Documentation

- [Design specification](docs/slimpowers/specs/2026-09-08-witchfire-randomizer-and-wiki-design.md)

## Legal

This is an unofficial fan project. Witchfire and all related assets are the property of their respective owners. Data and icons are sourced from the community wiki (witchfire.wiki.gg).
