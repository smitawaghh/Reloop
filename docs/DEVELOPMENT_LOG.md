# Development Log

## Backend quality pass (post Phase 5, pre Phase 6)

**What was built:** A validation/consistency pass on the existing backend,
plus a first real test suite. No new frameworks, no auth, no DTO/mapper
layers, no new architecture.

**Files changed:**
- `model/EwasteItem.java` — removed the duplicated `status` field
  (`getStatus`/`setStatus`); a device's pickup lifecycle is now tracked
  only on `Pickup`.
- `model/Pickup.java` — unchanged in shape; now the sole owner of
  `PickupStatus`.
- `service/EwasteService.java` — `registerItem()` now rejects a blank
  `location` via a new `requireNonBlank` helper (weight and condition were
  already validated by `setWeightKg()` and `parseCondition()`).
- `service/PickupService.java` — `requestPickup()` now rejects a blank
  `pickupLocation` and rejects a second pickup for an item that already
  has one in progress (`ActivePickupExistsException`, HTTP 409).
  `updateStatus()` no longer writes through to the item (nothing to sync
  now that status lives only on `Pickup`). The `PriorityQueue` javadoc was
  rewritten: it previously claimed the current implementation was
  asymptotically better than sorting, which is false because the method
  drains the queue fully to build the whole ordered response
  (`O(n log n)`, same as `Collections.sort`). The real justification for
  keeping `PriorityQueue` here is that it (a) makes "give me the next
  highest-priority item" explicit, and (b) would let a future
  poll-one-at-a-time version of this method reuse the same heap for a
  genuine `O(log n)` win.
- `repository/PickupRepository.java` — added
  `existsByItem_IdAndStatusNot(itemId, status)` to support the
  active-pickup check without loading entities.
- `exception/ActivePickupExistsException.java` — new, for the 409 case.
- `exception/GlobalExceptionHandler.java` — added a handler mapping
  `ActivePickupExistsException` to HTTP 409.
- `test/model/RewardCalculationTest.java` — new: Laptop/MobilePhone/Battery
  reward formulas, invalid weight.
- `test/service/EwasteServiceTest.java` — new: registration happy path,
  unknown item type, unknown condition, blank location, non-positive
  weight, item-not-found.
- `test/service/PickupServiceTest.java` — new: pickup creation, blank
  pickup location, duplicate active pickup rejection, priority ordering
  (asserts the PriorityQueue reorders four pickups supplied out of
  priority order into Battery → Laptop → MobilePhone → Accessory).

**Why:** The task list called out three real gaps: missing input
validation beyond weight, an unenforced "one active pickup per item"
business rule, and a status field duplicated across two classes that could
silently drift out of sync (e.g. `updateStatus` used to write to
`Pickup.status` and `EwasteItem.status` separately — a bug waiting to
happen the moment one of those two calls was forgotten). The PriorityQueue
comment was simply inaccurate and needed correcting rather than removing
the structure, since the intent behind using it is still legitimate.

**Tests executed:** `./mvnw test` — full Maven test suite, run to
completion.

**Test results:** `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` —
`BUILD SUCCESS`. Breakdown:
- `ReLoopApplicationTests`: 1 (Spring context loads)
- `RewardCalculationTest`: 4
- `EwasteServiceTest`: 6
- `PickupServiceTest`: 4

All of the above ran against real Mockito mocks/JUnit 5, not fabricated
output. Additionally spot-checked live against the running app on H2
(register item → request pickup → second pickup for same item → 409;
blank location → 400; blank pickup location → 400) — all matched the
unit-test expectations.

**Remaining backend limitations (known, not yet addressed):**
- No pagination on `/api/items` or `/api/pickups` — fine at portfolio
  scale, would matter at real scale.
- No `@Valid`/Bean Validation annotations — validation is done by hand in
  the service layer, which is consistent with "no unnecessary layers" but
  means validation logic isn't declarative.
- No CORS configuration yet — needed before the React frontend (Phase 6)
  can call this API from a different origin.
- No tests at the controller/HTTP layer (`@WebMvcTest`) yet — current
  tests cover services directly; the live curl smoke test above is the
  only end-to-end check so far.
- Recycler center / recycler identity has no dedicated entity — it's a
  free-text field on `Pickup`, deliberately, to avoid modeling a User/Org
  concept the spec doesn't ask for.

## Full completion pass: tests, CORS, React frontend, docs, deployment prep

**What was built:** Closed the remaining test-coverage gap, added CORS
config, built the entire React frontend from nothing, wrote the full
`docs/` set, rewrote the README, and prepared (but did not perform) a
deployment.

**Files changed / added (backend):**
- `test/model/RewardCalculationTest.java` — added the missing Accessory
  reward test (was already covering Laptop/MobilePhone/Battery).
- `test/service/PickupServiceTest.java` — added `updateStatus` tests
  (successful transition, pickup-not-found).
- `application.properties` — added `reloop.cors.allowed-origins`.
- `config/CorsConfig.java` — new. The one configuration class in the
  project; without it the browser blocks every request from the Vite dev
  server (5173) to the API (8080), since they're different origins. Reads
  allowed origins from the property above so a deployed frontend's real
  domain can be added via the `RELOOP_CORS_ALLOWED_ORIGINS` environment
  variable without touching code.

**Files added (frontend, all new — the directory was empty before this
pass):**
- `services/api.js` — single fetch client; every page imports from here.
  Reads `VITE_API_BASE_URL` at build time (`.env.development` =
  `http://localhost:8080`, `.env.production` = placeholder for the real
  deployed backend URL).
- `components/Navbar.jsx`, `StatCard.jsx`, `EwasteCard.jsx`,
  `StatusBadge.jsx`, `PickupTimeline.jsx`.
- `pages/Dashboard.jsx`, `RegisterEwaste.jsx`, `MyItems.jsx`,
  `Pickups.jsx`, `Impact.jsx`.
- `App.jsx` — a single `page` state variable switches between the five
  pages. No `react-router` dependency was added; five pages and one nav
  bar don't justify a routing library, and it keeps the dependency list
  at exactly React + Vite as the spec requires.
- `styles/style.css` — the entire visual design (green sustainability
  palette, cards, badges, timeline, a pure-CSS bar chart for the Impact
  page — no chart library).

**Notable design decision — item status on the frontend:** since
`EwasteItem` no longer carries its own status field (removed in the prior
pass to avoid duplicating `Pickup`'s lifecycle state), `MyItems.jsx`
derives each item's displayed status by matching it against the pickup
list (`buildStatusByItemId`), defaulting to a `NOT_REQUESTED` badge when
no pickup exists yet. This keeps the "Pickup owns pickup lifecycle"
decision consistent all the way to the UI instead of re-introducing a
duplicate status somewhere in the frontend.

**Why:** the task required a fully working, integrated, deployment-ready
system, not just a backend. CORS was a hard blocker — without it, no
frontend request could ever succeed regardless of how correct the
frontend code was. The frontend itself needed building from scratch since
`frontend/src/{components,pages,services,styles}` were empty directories
with no files in them at the start of this pass.

**Tests executed:** `./mvnw test` (after adding the two new test cases),
`./mvnw package` (backend build), `npm install` + `npm run build`
(frontend production build), plus a live integration check: both servers
started, a `curl` OPTIONS preflight against `/api/items` with
`Origin: http://localhost:5173` confirmed the CORS headers, and a `curl`
POST/GET against the live backend with that same Origin header confirmed
end-to-end request handling.

**Test results:**
- Backend: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` — `BUILD
  SUCCESS` for both `test` and `package`.
- Frontend: `vite build` completed with no errors (`✓ 42 modules
  transformed`, `dist/` produced).
- CORS preflight: `Access-Control-Allow-Origin: http://localhost:5173`
  confirmed present on the OPTIONS response.
- Live API calls with the frontend's Origin header: `POST /api/items` →
  201, `GET /api/items` → 200, `GET /api/impact` → 200.

**Not verified (explicitly, so as not to overclaim):** the rendered
frontend was never visually inspected in an actual browser — this
environment has no browser-automation tool available. What *was* verified
is that `npm run build` compiles the React code with no errors, and that
the exact HTTP calls `services/api.js` makes (same method, URL, headers,
and Origin) succeed against the real backend. A manual click-through in a
real browser is still worth doing before you consider the UI itself
proven, not just the code that calls the API.

**Remaining backend/frontend limitations (known, not yet addressed):**
- No pagination on any list endpoint.
- No Bean Validation annotations — hand-rolled checks only.
- No controller-layer (`@WebMvcTest`) tests — service-layer tests plus the
  manual curl smoke tests are the current coverage.
- No dedicated RecyclerCenter entity.
- Frontend has no loading skeletons/spinners beyond plain "Loading..."
  text, and no automated frontend tests (none were requested).
- Actual deployment was not performed — only prepared (see
  `README.md`'s Deployment section). No claim is made that the site is
  live anywhere.

## Domain model upgrade: User, roles, RecyclerCenter, pickup scheduling, transition rules, PostgreSQL

**What was built:** six frozen-scope changes to make the domain model more
realistic, with no authentication, no new frameworks, and no architecture
changes: (1) `User` entity, (2) Citizen/Recycler role awareness via a
frontend demo-mode switcher, (3) `RecyclerCenter` entity replacing a
free-text field, (4) pickup date/time scheduling, (5) validated pickup
status transitions, (6) PostgreSQL support alongside H2.

**Files added (backend):**
- `model/User.java`, `model/UserRole.java` — plain entity + enum, no
  password field. Table named `app_user` (not `user`, a reserved SQL
  keyword in both H2 and PostgreSQL — this broke `ReLoopApplicationTests`
  on first attempt with a `JdbcSQLSyntaxErrorException`, fixed by adding
  `@Table(name = "app_user")`).
- `model/RecyclerCenter.java` — plain entity (`id`, `name`, `location`).
- `repository/UserRepository.java`, `repository/RecyclerCenterRepository.java`.
- `exception/InvalidPickupStatusTransitionException.java` — new, mapped to
  HTTP 409 in `GlobalExceptionHandler`.
- `controller/UserController.java`, `controller/RecyclerCenterController.java`
  — minimal list/create, binding directly to the entities rather than a
  separate request class (justified: no logic beyond persisting what's
  sent, unlike the other controllers).
- `controller/AssignRecyclerRequest.java`, `controller/SchedulePickupRequest.java`
  — new request-body classes for the two endpoints below.
- `config/DemoDataSeeder.java` — a `CommandLineRunner` seeding one Citizen,
  one Recycler, and two recycler centers on first startup. Necessary
  because there's no signup flow to create these otherwise.

**Files changed (backend):**
- `model/EwasteItem.java` — added nullable `@ManyToOne owner` (User).
- `model/Pickup.java` — added nullable `@ManyToOne requestedBy` (User);
  replaced the free-text `recyclerCenter: String` with a `@ManyToOne
  RecyclerCenter`; added `pickupDate`/`pickupTime` (`java.time.LocalDate`/
  `LocalTime`).
- `controller/EwasteItemRequest.java`, `controller/PickupRequest.java` —
  added an optional `userId` field to each.
- `controller/EwasteController.java` — `GET /api/items` now accepts an
  optional `userId` query param, filtering to that owner's items.
- `controller/PickupController.java` — `POST /api/pickups` passes
  `userId` through; `GET /api/pickups` accepts an optional `userId`;
  `PUT /assign` now takes `{recyclerCenterId}` instead of a free-text
  name; added `PUT /api/pickups/{id}/schedule`.
- `service/EwasteService.java` — injects `UserRepository`; links `owner`
  when `userId` is supplied (404 if it doesn't exist); added
  `getItemsByOwner(userId)`.
- `service/PickupService.java` — the significant rewrite. Added a single
  `Map<PickupStatus, PickupStatus> VALID_NEXT_STATUS` (an `EnumMap`) as
  the one source of truth for which transitions are legal; a private
  `validateTransition(current, target)` helper is called by
  `assignRecycler` (must be PENDING), `scheduleDatetime` (must be
  ASSIGNED, new method), and `updateStatus` (checks whatever transition
  was requested) before any field is mutated. `assignRecycler` now looks
  up a real `RecyclerCenter` by id instead of accepting a string.
  `requestPickup` gained an optional `userId` param, linking
  `requestedBy`.
- `pom.xml` — added the `postgresql` JDBC driver (`runtime` scope,
  alongside the existing `h2` dependency).
- `application.properties` — removed the hardcoded
  `spring.datasource.driverClassName=org.h2.Driver` so Spring Boot infers
  the driver from the JDBC URL prefix instead (this is what lets the same
  properties file work for both H2 and PostgreSQL); added comments
  documenting the `SPRING_DATASOURCE_*` / `SPRING_JPA_HIBERNATE_DDL_AUTO`
  environment variable overrides for deployment.

**Files changed (frontend):**
- `services/api.js` — `getItems`/`getPickups` accept an optional `userId`;
  `assignRecycler` now sends `recyclerCenterId`; added `schedulePickup`,
  `getUsers`, `getRecyclerCenters`.
- `App.jsx` — loads the user list once, holds `currentUser` state
  (restored from `localStorage` by id across reloads), passes it down to
  the pages and `Navbar`.
- `components/Navbar.jsx` — added the "DEMO" user-switcher dropdown, with
  an explicit `title` and visual badge marking it as prototype-only, not a
  login.
- `pages/RegisterEwaste.jsx` — sends `currentUser.id` as `userId`; shows
  "Registering as: X".
- `pages/MyItems.jsx` — now scoped to `currentUser` (`getItems(userId)`,
  `getPickups(userId)`); shows nothing until a demo user is selected.
- `pages/Pickups.jsx` — the significant rewrite. Citizens see only their
  own pickups (`getPickups(userId)`); recyclers see every pickup and get
  the operator controls (`isRecycler = currentUser.role === 'RECYCLER'`).
  The old single "Assign" text input was replaced with a `RecyclerCenter`
  dropdown (fetched from `GET /api/recycler-centers`) shown only when a
  pickup is PENDING; a new date/time picker + "Schedule" button appears
  only when a pickup is ASSIGNED; the generic "Mark as X" button now only
  ever offers SCHEDULED→COLLECTED or COLLECTED→RECYCLED, since the other
  two transitions each have their own dedicated control.
- `styles/style.css` — added `.demo-user-switcher`/`.demo-badge` styles
  and a `.pickup-meta` line style for showing the assigned center/schedule.

**Why:** each change was one of the six items explicitly requested, kept
to the smallest change that satisfied it — e.g. `UserController`/
`RecyclerCenterController` skip a service layer because there's no
business logic to put in one (just list/create), while `PickupService`
needed the transition table because "reject invalid status changes" is
exactly business logic. The free-text→entity change for `RecyclerCenter`
was necessary because a `PUT /assign {recyclerCenterId}` needs something
to validate the id against (a 404 for an unknown center), which a string
field can't provide.

**Tests executed:** `./mvnw test`, `./mvnw package`, `npm run build`, plus
a live end-to-end smoke test against the running app: registered an item
as the seeded Citizen (`userId=1`), confirmed `GET /api/items?userId=1`
scoped correctly, requested a pickup, then walked the full lifecycle -
rejected `PENDING→COLLECTED` (409), assigned a recycler center
(`PENDING→ASSIGNED`), rejected a second assign attempt while already
ASSIGNED (409), scheduled a date/time (`ASSIGNED→SCHEDULED`), rejected
skipping to RECYCLED (409), advanced `SCHEDULED→COLLECTED`, rejected
moving backwards to ASSIGNED (409), and finally advanced
`COLLECTED→RECYCLED`.

**Test results:**
- First `./mvnw test` run failed: `ReLoopApplicationTests` couldn't load
  the Spring context because `user` is a reserved SQL keyword — fixed
  with `@Table(name = "app_user")` on `User`. Existing `PickupServiceTest`
  tests also failed to compile against the new constructor signatures and
  method parameters — updated to match, plus added new tests for
  `assignRecycler`/`scheduleDatetime`/every valid and several invalid
  transitions (skip-forward, move-backward, change-after-terminal).
- After fixes: **`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0` —
  `BUILD SUCCESS`** for both `test` and `package`.
- Frontend: `npm run build` succeeded with no errors.
- Live smoke test: every expected 200/201 and every expected 409 matched
  exactly (see above), and the JSON responses showed the `owner`,
  `requestedBy`, and `recyclerCenter` objects correctly populated/null as
  expected at each stage.

**Remaining limitations (known, not yet addressed):**
- Still no authentication or authorization — see
  `docs/PROJECT_OVERVIEW.md` and `docs/INTERVIEW_QA.md` Q35/Q40.
- No migration tool for the PostgreSQL path — `ddl-auto` only.
- No controller-layer (`@WebMvcTest`) tests.
- Frontend UI still not visually verified in an actual browser in this
  environment (same limitation as the prior pass) — only the build and
  the real HTTP integration were verified.
- PostgreSQL itself was not actually connected to or tested in this pass
  (no PostgreSQL instance was available in this environment) — only that
  the driver is on the classpath and the configuration is
  environment-variable-driven with no hardcoded driver class. The H2 path
  was fully exercised; the PostgreSQL path is configuration-ready but
  unverified end-to-end.
