# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`autotradebtc` is a Spring Boot 4.0.7 (Java 17) application, currently a freshly generated skeleton with no
business logic yet (only the default `@SpringBootApplication` entry point and a context-loads test).

- Group/artifact: `com.mamta.btctrade:autotradebtc`
- Base package: `com.mamta.btctrade.autotradebtc`
- Web stack: `spring-boot-starter-webmvc` (Spring MVC on Servlet stack)
- Persistence: `mysql-connector-j` on the runtime classpath, but no datasource is configured yet in
  `src/main/resources/application.properties` and no JPA/JDBC starter or entities exist yet.
- Lombok is available (annotation processing is wired into both the `default-compile` and `default-testCompile`
  executions in `pom.xml` — if Lombok config ever needs adjusting, it must be updated in both places).

## Commands

Use the Maven wrapper (`./mvnw`), not a system-installed Maven.

```bash
# Build (compiles + runs tests)
./mvnw verify

# Compile only
./mvnw compile

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AutotradebtcApplicationTests

# Run a single test method
./mvnw test -Dtest=AutotradebtcApplicationTests#contextLoads

# Run the application locally
./mvnw spring-boot:run

# Package the runnable jar
./mvnw package
```

## Frontend

The UI lives in `frontend/` — a Vite + React 19 + TypeScript app (Tailwind CSS v4 for styling,
TanStack Query for data fetching/polling, lucide-react for icons). It is **not** wired into the
Maven build; after changing anything under `frontend/src`, rebuild it manually so the compiled
output lands in `src/main/resources/static` (which Spring Boot serves as static content):

```bash
cd frontend
npm install       # first time / after dependency changes
npm run dev        # dev server with API proxy to localhost:8080 (run the Spring app separately)
npm run build       # type-checks, then builds straight into ../src/main/resources/static
npm run lint        # oxlint
```

`src/main/resources/static` is generated output, checked in so the jar has something to serve
without requiring a frontend build step in CI — always regenerate it via `npm run build` rather
than hand-editing it.

Key frontend structure:
- `src/api/` — typed `fetch` client + response types mirroring the backend DTOs
- `src/hooks/` — TanStack Query hooks (`useWallets`, `useWhales`, mutations)
- `src/components/wallets/` — wallet CRUD + BTC transactions / Hyperliquid fills / open orders tables
- `src/components/whales/` — the whale tracker view (`WhaleRow` renders address/label on the left
  and account value / 30d PnL / all-time profit right-aligned on the right, per product requirement)
