# Architecture

```
React Frontend (Vite, plain CSS)
        |
        |  REST / JSON  (fetch, via services/api.js)
        v
Spring Boot Backend
        |
        v
   Controller   <-- parses HTTP request, delegates, returns response
        |
        v
    Service     <-- business logic: validation, reward orchestration,
        |            priority ordering, status transitions
        v
  Repository    <-- Spring Data JPA interfaces (no SQL written by us)
        |
        v
   Database     (H2 locally, PostgreSQL for deployment - same code, see DATABASE.md)
```

## Layer responsibilities

**Frontend** (`frontend/src`)
- `components/` — small reusable pieces (Navbar, StatCard, EwasteCard,
  StatusBadge, PickupTimeline). No business logic; they render props.
- `pages/` — one component per screen (Dashboard, RegisterEwaste, MyItems,
  Pickups, Impact). Each page owns its own data fetching (`useEffect` +
  `useState`) and calls `services/api.js`.
- `services/api.js` — the *only* place that knows the backend's URL and
  endpoint shapes. Every page imports `api` from here rather than calling
  `fetch` directly, so there's a single point to change if an endpoint
  moves.
- `App.jsx` — holds one `page` state and renders whichever page is
  selected. Deliberately not using a routing library: five pages and one
  nav bar don't need one. It also loads the demo user list once (`GET
  /api/users`) and holds `currentUser` - the frontend's stand-in for "who
  is logged in," with no real login behind it (see PROJECT_OVERVIEW.md).

**Controller** (`backend/.../controller`)
- One controller per resource: `EwasteController`, `PickupController`,
  `ImpactController`, `UserController`, `RecyclerCenterController`.
- Reads the request body/path variables, calls exactly one service method,
  returns the result. No business logic, no repository calls.
- `EwasteItemRequest` / `PickupRequest` / `AssignRecyclerRequest` /
  `SchedulePickupRequest` are plain request-body classes — not a
  DTO/mapper layer, just the shape the client sends. `UserController` and
  `RecyclerCenterController` bind directly to the `User`/`RecyclerCenter`
  entities instead, since those two endpoints are simple enough (list,
  create) that an extra request class would add nothing.

**Service** (`backend/.../service`)
- `EwasteService` — validates and constructs the correct `EwasteItem`
  subclass, reads items back (optionally scoped to one owner), links a
  `User` as owner when a `userId` is supplied.
- `PickupService` — validates and creates pickups (optionally linking a
  requesting `User`), blocks a second active pickup for the same item,
  exposes the priority-ordered pending list, and owns the single
  transition-validation table (`VALID_NEXT_STATUS`) that every
  status-changing method (`assignRecycler`, `scheduleDatetime`,
  `updateStatus`) checks against before mutating anything.
- `ImpactService` — aggregates dashboard/impact numbers across all items
  and pickups.
- There is deliberately no `UserService`/`RecyclerCenterService` — those
  two resources are simple enough (list + create, no business rules) that
  their controllers talk to the repository directly; adding a service
  layer with no logic in it would be an empty abstraction.
- This is where `ArrayList`/`HashMap`/`PriorityQueue` usage lives — see
  `DATA_FLOW.md` and the inline comments in `PickupService`/`EwasteService`
  for why each was chosen.

**Repository** (`backend/.../repository`)
- `EwasteItemRepository`, `PickupRepository`, `UserRepository`,
  `RecyclerCenterRepository` — all just `JpaRepository` interfaces. Spring
  Data generates the implementation (a dynamic proxy) from the interface +
  method name at startup. Derived-query methods
  (`findByStatus`, `existsByItem_IdAndStatusNot`, `findByOwner_Id`,
  `findByRequestedBy_Id`, `findByRole`) — no `@Query`/SQL needed for
  filters this simple.

**Model** (`backend/.../model`)
- The OOP hierarchy — see `OOP_CONCEPTS.md`. `User` and `RecyclerCenter`
  sit outside that hierarchy on purpose - they're plain, unrelated domain
  entities, not additional `EwasteItem` subclasses, because they don't
  share the hierarchy's identity ("a device being recycled").

**Exception handling** (`backend/.../exception`)
- Four custom exceptions (`InvalidItemException`,
  `ResourceNotFoundException`, `ActivePickupExistsException`,
  `InvalidPickupStatusTransitionException`) plus one
  `@RestControllerAdvice` (`GlobalExceptionHandler`) that maps each to an
  HTTP status. Controllers never catch these themselves — they just let
  them propagate.

**Config** (`backend/.../config`)
- `CorsConfig` — because the browser blocks cross-origin requests from the
  Vite dev server (port 5173) to the API (port 8080) without it. Allowed
  origins are read from `application.properties`
  (`reloop.cors.allowed-origins`), overridable by the
  `RELOOP_CORS_ALLOWED_ORIGINS` environment variable in production.
- `DemoDataSeeder` — a `CommandLineRunner` that seeds one Citizen user, one
  Recycler user, and two recycler centers on first startup (only if none
  already exist). Necessary because there's no login/signup flow to create
  these otherwise, and the frontend's demo-mode switcher needs at least
  one of each to show.

## Request flow example (register an item)

1. User submits the Register E-Waste form in React.
2. `RegisterEwaste.jsx` calls `api.registerItem(payload)`.
3. `services/api.js` sends `POST http://<backend>/api/items`.
4. `EwasteController.registerItem` receives the body as an
   `EwasteItemRequest`.
5. `EwasteService.registerItem` validates fields, picks the matching
   `EwasteItem` subclass (`Laptop`/`MobilePhone`/`Battery`/`Accessory`),
   constructs it (which validates weight), links the demo `User` as owner
   if a `userId` was sent, saves it via `EwasteItemRepository`.
6. Hibernate inserts a row into `ewaste_item` (single-table inheritance —
   see `DATABASE.md`).
7. The saved entity (including its computed `estimatedReward`) is
   serialized to JSON and returned with HTTP 201.
8. React shows the success screen with the item's ID and estimated reward.
