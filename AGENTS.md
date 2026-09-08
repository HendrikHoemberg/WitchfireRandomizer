<!-- SLIMPOWERS_SKELETON_BEGIN -->
# Project Instructions

The framework is active globally (skills + workflow rules are injected into every session), so
this file only carries **project-specific** facts. Add and keep it up to date.

## Commands
- **Verify (everything)**: `./mvnw test`
- **Single test**: `./mvnw test -Dtest=TestClassName#testMethodName`
- **Compile**: `./mvnw test-compile`

## Architecture
Spring Boot 4 / Java web application using Thymeleaf, HTMX, and Alpine.js for Witchfire Wiki and Loadout Randomizer. Data sourced from the Witchfire wiki.

## Conventions
- Test style: JUnit 5, MockMvc / SpringBootTest.
- Web UI: HTMX for server-driven partial page updates, Alpine.js for purely local client state/interactivity.
- Naming: Standard Spring Boot conventions (controllers, services, models/records, repositories/providers).

## Boundaries
- Do not edit: `.mvn/wrapper/*`, generated build artifacts.
<!-- SLIMPOWERS_SKELETON_END -->
