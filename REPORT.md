# CampusOA — Learning Report

> Work in progress: this file is updated after every milestone. The final version adds the
> architecture walkthrough, codebase tour, interview prep, exercises and glossary.

## 2. Build timeline

| Milestone | Commit | What changed | Problems hit and fixes |
|---|---|---|---|
| M0 Plan | `8befdad` | PLAN.md, .gitignore | The machine had no JDK, Docker or `gh`. I installed them with winget. Docker then failed with `dockerDesktopLinuxEngine` not found, because WSL wasn't installed. You ran `wsl --install` and rebooted. |
| M1 Skeleton | `eb97c73` | Spring Boot 3.5.16 / Java 21, Maven Wrapper, Flyway, actuator, springdoc, `ApiError` + `GlobalExceptionHandler`, `PageResponse`, Testcontainers base class, compose, `.env.example` | Spring Initializr no longer offers Boot 3.x (only 4.x), so I took only the Maven Wrapper from it and wrote `pom.xml` by hand against Boot **3.5.16** (still published to Maven Central). The first Testcontainers run took ~110 s because it had to pull `postgres:16-alpine`. Later runs take seconds. |
| M2 Schema | `76e0faf` | V1–V4 migrations: 27 tables, CHECK/FK rules, generated `tsvector` columns, partial and GIN indexes, seed data | Spring Session's table DDL was copied from the jar (`schema-postgresql.sql`) into a Flyway migration so Flyway owns *all* schema. `MigrationIT` proves the constraints really reject bad rows. |
| M4 Drives & companies | `218ee43` | Company list/detail (LATERAL-join SQL), admin company and stat editing, drives with stages, criteria, merge, calendar events, the pure `Eligibility` rules | **Bug 1:** `academic_year char(7)` failed Hibernate's `ddl-auto: validate` (it expected `varchar`). V1 had never run against a persistent DB, so I changed it in place to `varchar(7)`. After deployment the rule is *never edit a migration*, only add a new one. **Bug 2:** a Java text block strips common indentation, which ate the leading space before `AND (NOT EXISTS ...`. The SQL became `d.hiddenAND(...)`, and Postgres reported `schema "d" does not exist`. Fixed with an explicit `" " +`. The integration test caught it, which is exactly why we test SQL against a real Postgres. |
| M3 Auth | `dd34c80` | Google OIDC login, allow-list, roles, JDBC sessions, SPA CSRF, CORS, CSP, rate limiting, `/api/me`, account deletion, admin roles/bans | Spring Security 6.5 has no `csrf().spa()` shortcut (that arrives in 7.x), so `SpaCsrf.java` implements the handler from the reference docs. |

## 11. Decisions & assumptions

| # | Decision | Alternatives considered | Why |
|---|---|---|---|
| D1 | **Spring Boot 3.5.16**, not 4.x | Boot 4.0/4.1 (current on Initializr) | You asked for Boot 3, and nearly all tutorials you'll read use 3.x. The 3.5 line still receives patch releases on Maven Central. Upgrading to 4 is listed as an exercise. |
| D2 | **Server sessions in Postgres** (Spring Session JDBC), not JWT | Stateless JWT in `localStorage` or a cookie | HttpOnly cookie that XSS can't read; sessions can be revoked instantly (bans, role changes); no refresh-token logic; survives Render free-tier restarts. |
| D3 | **Same-origin via Vercel rewrites** | Browser calls Render directly with CORS + `SameSite=None` cookies | Third-party cookies are blocked on iOS Safari, and most users are on phones. |
| D4 | Private CGPA/backlogs in a **separate table** | Columns on `app_user` | A query has to opt in to private data with a JOIN, and the `AuthorDto` type has no such fields at all. |
| D5 | `report` has **three nullable FKs + `CHECK num_nonnulls(...) = 1`** | One `target_type` + `target_id` pair (polymorphic) | Keeps real foreign keys, so cascades and integrity are enforced by the database. |
| D6 | "Up to 9.5 LPA" stored as `ctc_min_lpa = NULL`, `ctc_max_lpa = 9.5` | A boolean `ctc_is_upto` (as PLAN.md said) | One fewer column, and the meaning follows from the range itself. *This changed from PLAN.md.* |
| D7 | Empty `ALLOWED_EMAIL_DOMAINS` means **nobody** can sign in | Treat empty as "anyone" | Fail closed: a missing env var must not open the site to every Gmail user. |
| D8 | The scraper User-Agent comes from the `SCRAPER_USER_AGENT` env var | Hard-code your GitHub username | Keeps personal identifiers out of code. You set it to include your repo URL. |
| D9 | `ADMIN_EMAILS` **promotes but never demotes** | Sync the role on every login | An admin can't lose access just because an env var was edited by mistake. Demotion is an explicit admin action. |
| D10 | Changing a role or banning **revokes all of that user's sessions** | Re-check the role from the DB on every request | Simple, and the effect is immediate. The cost is that the user has to sign in again. |
