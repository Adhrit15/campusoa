# Contributing to CampusOA

Thanks for helping! CampusOA is built by students, for students. You don't need to be an expert:
small fixes, docs and tests are very welcome.

## Ground rules (please read)

1. **Unofficial project.** Don't add CBIT logos or anything that looks official.
2. **No personal data about other students.** Never commit or import names, roll numbers or results
   from placement documents. Company-level aggregates only.
3. **No secrets in Git.** Use `.env` (git-ignored). If you commit a secret by accident, tell a
   maintainer immediately so it can be rotated. Deleting the commit is not enough.

## Getting set up

You need **JDK 21**, **Node 22+** and **Docker Desktop**. See the README's "Run it locally" section.
The quickest path:

```bash
docker compose up -d db                      # Postgres
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev    # http://localhost:5173
```

With the `dev` profile you can sign in as demo users from **/login**, so you don't need Google OAuth credentials.

## Workflow

1. Pick an issue (or open one describing what you want to change). Comment so nobody duplicates work.
2. Create a branch: `git switch -c feat/company-compare` (prefixes: `feat/`, `fix/`, `docs/`, `test/`, `chore/`).
3. Make small commits with [Conventional Commit](https://www.conventionalcommits.org/) messages:
   `feat(prep): add weekly goal`, `fix(drives): handle missing date`, `docs: explain leaderboard SQL`.
4. Run the tests before pushing:
   ```bash
   cd backend && ./mvnw verify          # needs Docker for Testcontainers
   cd frontend && npm run typecheck && npm test
   ```
5. Open a pull request. Describe what and why, and add screenshots for UI changes. CI must be green.
6. A maintainer reviews it. Expect questions: reviews are how we all learn.

## Code conventions

**Backend (Java / Spring)**
- Feature packages (`drive/`, `company/`, `prep/` …), each with controller → service → repository.
- Controllers stay thin. Business rules go in services, and SQL goes in `*Queries` classes or repositories.
- **Never return entities from controllers.** Map them to records in `*Dtos.java`.
- Throw `ApiException` for expected errors. `GlobalExceptionHandler` formats them.
- Schema changes go in a **new** Flyway migration (`V5__add_x.sql`). Never edit an applied migration.
- Every new endpoint needs a role rule (URL rule in `SecurityConfig` and/or `@PreAuthorize`) and a test.
- Pure logic (like `Eligibility`, `Streaks`) gets fast unit tests. SQL gets Testcontainers integration tests.

**Frontend (React / TypeScript)**
- Data fetching goes through hooks in `src/api/hooks.ts` (TanStack Query), never raw `fetch` in components.
- Keep API types in `src/api/types.ts` in sync with the backend DTOs.
- Use Tailwind with the component classes in `index.css` (`card`, `btn-primary`, `input` …).
- Mobile first: check every page at 375 px width and in dark mode.
- Render user content only through `<Markdown>` (it sanitises HTML).

## Moderators

Moderators use **/admin → Moderation** to review reports. Follow the in-app posting guidelines.
When in doubt, hide rather than remove, and leave a note explaining the decision.
