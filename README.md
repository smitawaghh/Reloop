# ReLoop ♻️

Give your old electronics a second life.

ReLoop is a community e-waste collection and recycling platform. Citizens
register unwanted devices, request a pickup, track its status, and see
estimated eco-points. A recycler operator view lets someone prioritize
hazardous items, assign a recycler center, schedule a pickup date/time,
and advance pickup status through a validated lifecycle.

Built as a university fresher portfolio project, deliberately kept small
enough to explain every class in an interview — see `docs/` for full
write-ups on architecture, OOP concepts, data flow, database design, the
API, and 40 interview questions with answers grounded in the actual code.

## Features

- Register e-waste (Laptop / Mobile Phone / Battery / Accessory) with
  weight, condition, location, description.
- Estimated eco-points reward — calculated differently per device type
  via polymorphism, not a shared formula (see `docs/OOP_CONCEPTS.md`).
- Request a pickup; one active pickup per item is enforced.
- Pickup lifecycle with **validated transitions**: PENDING → ASSIGNED →
  SCHEDULED → COLLECTED → RECYCLED. Skipping a step or moving backwards is
  rejected with HTTP 409 (see `docs/API_REFERENCE.md`).
- Recycler view: pending pickups ordered by hazard priority
  (Battery > Laptop > Mobile Phone > Accessory), assign a real
  `RecyclerCenter`, schedule a pickup date/time, advance status.
- `User` domain entity with `CITIZEN`/`RECYCLER` roles — items and
  pickups can be linked to a user. **This is not a login system** — see
  Limitations.
- Dashboard and Impact pages with aggregate statistics, explicitly labeled
  as estimates.

## Tech stack

- **Frontend**: React + Vite, plain CSS. No UI framework, no state
  management library, no router (five pages are handled by one piece of
  state in `App.jsx`).
- **Backend**: Java, Spring Boot (Web + Data JPA), Maven.
- **Database**: H2 locally (in-memory, resets on restart), PostgreSQL for
  deployment — same JPA code, only configuration differs. See
  `docs/DATABASE.md`.
- **Communication**: REST/JSON.

Deliberately **not** used: Spring Security/JWT/OAuth, Docker, Redis,
Kafka, GraphQL, Redux, any CSS framework, any chart library, any cloud
SDK. See `docs/PROJECT_OVERVIEW.md` for why.

## Architecture

```
React Frontend  --REST/JSON-->  Controller --> Service --> Repository --> Database
```

Full breakdown of each layer's responsibility in `docs/ARCHITECTURE.md`.

## Folder structure

```
reloop/
├── backend/           Spring Boot app (Maven)
│   └── src/main/java/com/reloop/
│       ├── model/       EwasteItem hierarchy, Pickup, User, RecyclerCenter, enums
│       ├── repository/  Spring Data JPA interfaces
│       ├── service/      business logic (incl. pickup transition validation)
│       ├── controller/   REST endpoints + request classes
│       ├── exception/    custom exceptions + global handler
│       └── config/       CORS configuration + demo data seeder
├── frontend/           React app (Vite)
│   └── src/
│       ├── components/  Navbar (incl. demo-mode user switcher), StatCard,
│       │                 EwasteCard, StatusBadge, PickupTimeline
│       ├── pages/        Dashboard, RegisterEwaste, MyItems, Pickups, Impact
│       ├── services/     api.js (the only file that knows the backend URL)
│       └── styles/       style.css
└── docs/               PROJECT_OVERVIEW, ARCHITECTURE, OOP_CONCEPTS,
                         DATA_FLOW, DATABASE, API_REFERENCE,
                         DEVELOPMENT_LOG, INTERVIEW_QA
```

## Local setup

### Backend

```
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080` against H2 by default. H2 console at
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:reloopdb`, user
`sa`, no password). On first startup, two demo users (one CITIZEN, one
RECYCLER) and two recycler centers are seeded automatically.

### Frontend

```
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and reads `VITE_API_BASE_URL` from
`.env.development` (defaults to `http://localhost:8080`).

Both servers need to be running for the app to work end to end. On first
load, use the "DEMO" dropdown in the nav bar to switch between the seeded
Citizen and Recycler users — this drives which pages/controls are shown,
but is **not** a real login (see Limitations).

## API overview

Full request/response documentation in `docs/API_REFERENCE.md`. Summary:

| Method | Endpoint                     | Purpose                                |
|--------|------------------------------|------------------------------------------|
| POST   | `/api/items`                 | register a new e-waste item (optional `userId`) |
| GET    | `/api/items?userId=`         | list items (all, or one citizen's own)  |
| GET    | `/api/items/{id}`            | fetch one item                          |
| GET    | `/api/items/recent`          | recent items (dashboard feed)           |
| POST   | `/api/pickups`               | request a pickup (optional `userId`)    |
| GET    | `/api/pickups?userId=`       | list pickups (all, or one citizen's own)|
| GET    | `/api/pickups/{id}`          | fetch one pickup                        |
| GET    | `/api/pickups/pending`       | pending pickups, hazard-priority order  |
| PUT    | `/api/pickups/{id}/assign`   | PENDING → ASSIGNED + set recycler center|
| PUT    | `/api/pickups/{id}/schedule` | ASSIGNED → SCHEDULED + set date/time    |
| PUT    | `/api/pickups/{id}/status`   | SCHEDULED→COLLECTED or COLLECTED→RECYCLED |
| GET    | `/api/impact`                | aggregate dashboard/impact stats        |
| GET/POST | `/api/users`               | list / create demo users                |
| GET/POST | `/api/recycler-centers`    | list / create recycler centers          |

Any pickup status change that skips a step or moves backwards returns
`409 Conflict` regardless of which endpoint is used — see
`docs/API_REFERENCE.md` "Valid transitions".

## Testing

Backend:
```
cd backend
./mvnw test        # 30 tests: reward formulas, validation, priority
                    # ordering, duplicate-pickup prevention, valid/invalid
                    # pickup transitions, User/RecyclerCenter linkage,
                    # not-found handling
./mvnw package      # produces backend/target/reloop-0.0.1-SNAPSHOT.jar
```

Frontend:
```
cd frontend
npm run build       # production build to frontend/dist/
```

Both were run and passed as of the last development pass — see
`docs/DEVELOPMENT_LOG.md` for exact output, including a live end-to-end
smoke test of the full pickup lifecycle (assign → schedule → collect →
recycle) and its invalid-transition rejections. The frontend's rendered UI
was **not** visually verified in an actual browser (no browser-automation
tool was available in the environment this was built in) — only the
build and the real HTTP calls between the two servers were verified.

## Deployment

No hosting-specific SDK or Docker setup is included by design (see
Limitations below). To deploy:

**Backend**: package with `./mvnw package`, run the resulting jar
(`java -jar backend/target/reloop-0.0.1-SNAPSHOT.jar`) on any host that
can run a JVM. Set:
- `RELOOP_CORS_ALLOWED_ORIGINS` — your deployed frontend's real origin(s),
  comma-separated (defaults to `http://localhost:5173` — production
  requests from anywhere else are blocked by CORS until this is set).
- `SPRING_DATASOURCE_URL` — e.g. `jdbc:postgresql://<host>:5432/<db>`, to
  point at a real PostgreSQL instance instead of H2 (no code change
  needed; the JDBC driver is auto-detected from this URL).
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` — your
  PostgreSQL credentials. Never commit these — set them as environment
  variables on the host.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update` — so a restart doesn't wipe your
  deployed data the way the local H2 default (`create-drop`) does.

**Frontend**: set `VITE_API_BASE_URL` in `frontend/.env.production` (or as
a build-time environment variable on your hosting platform) to your
deployed backend's real URL, then run `npm run build`; deploy the
resulting `frontend/dist/` directory to any static host.

This project has not itself been deployed anywhere — the above is the
prepared path, not a claim that a live URL exists.

## Limitations

- **No authentication** — the nav bar's "DEMO" dropdown lets anyone switch
  to any seeded user, including the Recycler role; the backend does not
  verify who is making a request. Out of scope by design — see
  `docs/INTERVIEW_QA.md` Q35/Q40 for what a real version would need.
- H2 is in-memory locally; all data is lost on backend restart there
  (PostgreSQL in a real deployment doesn't have this problem).
- Reward/eco-point values are prototype assumptions, not real market
  recycling prices or scientific environmental figures.
- No pagination on list endpoints.
- No migration tool (Flyway/Liquibase) — `ddl-auto` only.

See `docs/PROJECT_OVERVIEW.md` and `docs/DEVELOPMENT_LOG.md` for the full
list and reasoning.
