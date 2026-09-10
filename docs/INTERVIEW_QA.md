# Interview Q&A

Answers correspond to the actual code in this repository, not idealized
descriptions.

### Core Java / OOP

**1. Why is `EwasteItem` abstract?**
Because a generic, typeless "e-waste item" has no defined reward formula —
it only makes sense to instantiate a concrete device type. Marking it
`abstract` makes that unrepresentable state impossible to create.

**2. Where's encapsulation in this project?**
`EwasteItem`'s fields are all `private`; `setWeightKg()` throws
`IllegalArgumentException` for `<= 0`, so an invalid weight can't exist on
any `EwasteItem` regardless of which constructor or code path created it.

**3. Where's inheritance, and why an abstract class instead of an
interface?**
`Laptop`/`MobilePhone`/`Battery`/`Accessory` extend `EwasteItem`, sharing
real instance fields (`weightKg`, `condition`, etc.) and a concrete method
(`conditionMultiplier()`). An interface can declare method signatures but
can't hold that shared field state, so an abstract class is the right
tool here.

**4. Where's polymorphism, concretely?**
`EwasteService` calls `item.calculateReward()` on an `EwasteItem`
reference; Java dispatches to whichever subclass's override matches the
object's actual runtime type. `Battery` is the clearest example: it
*skips* `conditionMultiplier()` entirely (hazard handling cost doesn't
scale with condition) while the other three use it — a real difference in
logic, not just different constants.

**5. Where's composition, and why not inheritance there?**
`Pickup` holds an `EwasteItem` via `@ManyToOne` rather than extending it.
A pickup *has* a device; it isn't a *kind of* device — deleting a pickup
must never delete the device it references.

**6. Could the four reward formulas have been done with one class and a
switch statement instead?**
Yes for three of them (they're just different constants). `Battery`
argues against it: it doesn't merely use different numbers, it omits an
entire step (`conditionMultiplier()`) that the others use — that's a
difference in control flow, which is exactly the case polymorphism suits
better than a parameterized branch.

**7. What's the difference between abstraction and encapsulation in this
codebase?**
Encapsulation hides *data* (private fields, validated setters).
Abstraction hides *implementation* (callers invoke `calculateReward()`
without knowing which formula runs).

### Collections / Data Structures

**8. Why `ArrayList` for `EwasteService.getRecentItems()`?**
Only sequential, insertion-order access is needed (a "recent activity"
feed) — no key lookup, so a `List` is right; `ArrayList` over
`LinkedList` because we only append, never insert in the middle.

**9. Why `HashMap` in `ImpactService`?**
Tallying total weight per device type is "look up this type's running
total, add to it" per item — an O(1) average-case get+put, which is
exactly what a `HashMap` is for. No ordering requirement exists that would
justify a `TreeMap`.

**10. Why `PriorityQueue` for pending pickups?**
It models "give me the next highest-priority item" directly — batteries
(hazardous) need collecting before laptops, which need collecting before
phones and accessories. `Comparator.comparingInt(Pickup::getPriorityWeight)`
orders by that field, lower = more urgent.

**11. Is the current `PriorityQueue` use actually faster than sorting?**
Be honest here: **no, not as currently implemented.**
`getPendingPickupsByPriority()` adds every pending pickup to the queue and
then drains it completely to build the full ordered response - that's
`O(n log n)`, the exact same asymptotic cost as
`Collections.sort(pending, comparator)`. The `PriorityQueue` wins over
sorting only when you repeatedly need just the *next* single
highest-priority element without re-deriving the whole order each time
(`O(log n)` per poll) - a genuine future extension of this method (e.g.
"give a recycler their next pickup one at a time as they finish the
previous one") would get that benefit; the current all-at-once endpoint
does not.

**12. What's the time complexity of `PriorityQueue.offer()`/`poll()`/`peek()`?**
`offer`/`poll`: `O(log n)`. `peek`: `O(1)`.

**13. Why not just use a `TreeSet` or sort a `List` instead of
`PriorityQueue`?**
For the *current* full-list-every-time use case, sorting the `List`
directly would have been equally correct and no worse asymptotically -
using `PriorityQueue` here is really about making the intent ("priority
order matters") explicit in the code and keeping the door open for an
incremental version later, not a performance requirement today.

### Spring Boot / REST / HTTP

**14. Why Spring Boot?**
Convention-over-configuration for a REST + JPA app: embedded Tomcat, an
auto-configured `DataSource`/`EntityManagerFactory` from a few properties,
and repository proxy generation, all without hand-wiring any of it.

**15. Why REST/JSON instead of something else?**
It's the natural fit for a browser-based frontend calling a stateless
backend over HTTP - no need for anything heavier (gRPC, GraphQL) at this
scale, and JSON maps directly onto the plain request/response classes
used here.

**16. Walk me through Controller → Service → Repository.**
Controller: parses the HTTP request into a plain request object, calls
exactly one service method, returns the result (Spring serializes it to
JSON). Service: validation and business logic - the only layer that
constructs domain objects or makes decisions. Repository: a `JpaRepository`
interface; Spring Data generates the SQL/JPQL from the interface's method
names, no implementation code written by hand.

**17. How is exception handling centralized?**
Three custom exceptions
(`InvalidItemException`/`ResourceNotFoundException`/`ActivePickupExistsException`)
plus one `@RestControllerAdvice` class (`GlobalExceptionHandler`) with an
`@ExceptionHandler` method per exception type, each returning the right
HTTP status (400/404/409). Controllers never catch these themselves.

**18. What HTTP status codes does the API use, and why?**
`201` on successful creation (POST /items, POST /pickups), `200` on
successful reads/updates, `400` for invalid input (bad enum value, blank
required field, non-positive weight), `404` for a missing id, `409` when
an item already has an active pickup (a real conflict, not just bad
input).

**19. How does CORS work here, and why was it needed?**
Without it, the browser's same-origin policy blocks a request from the
React dev server (`http://localhost:5173`) to the API
(`http://localhost:8080`) since they're different origins. `CorsConfig`
registers `/api/**` as open to the origins listed in
`reloop.cors.allowed-origins` (overridable via the
`RELOOP_CORS_ALLOWED_ORIGINS` env var in production).

### JPA / H2

**20. How does single-table inheritance work for `EwasteItem`?**
`@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` plus
`@DiscriminatorColumn` puts every subclass's rows in one `ewaste_item`
table with an `item_type` column recording which concrete class each row
represents. Chosen because all four subclasses share the exact same
columns - no subclass-only fields that would justify a join-per-subclass
strategy.

**21. Why H2 instead of PostgreSQL for this project?**
Zero setup - no install, no running service, works the moment you run the
app. The tradeoff, made explicit in the docs, is that
`ddl-auto=create-drop` means all data is wiped on every restart. A real
deployment would swap in PostgreSQL without touching any JPA code, only
`application.properties` and the JDBC dependency.

**22. What does `JpaRepository<EwasteItem, Long>` give you for free, and
what did you still have to write?**
Free: `save`, `findById`, `findAll`, `delete`, etc. Still had to write:
one derived-query method per repository where a specific filter was
needed (`findByStatus`, `existsByItem_IdAndStatusNot`) - Spring Data
parses those method names into queries automatically, so even those
didn't need hand-written SQL.

### React / Frontend integration

**23. Why no Redux/Context for state management?**
Each page owns only the data it displays, fetched with `useEffect` +
`useState` when the page mounts. There's no state shared *across* pages
that would justify a global store.

**24. Why no react-router?**
Five pages and one nav bar are handled by a single `page` state variable
in `App.jsx` and a conditional render - adding a routing library for that
would be extra dependency weight with no real benefit at this scale.

**25. How does the frontend know the backend's URL?**
`services/api.js` reads `import.meta.env.VITE_API_BASE_URL`, set per
environment via `.env.development` (`http://localhost:8080`) and
`.env.production` (the real deployed backend URL, set before running
`npm run build`). No URL is hard-coded anywhere else in the app.

**26. How are loading/error/empty states handled?**
Each page keeps `loading`/`error` state around its `fetch` calls, shows a
plain loading message while pending, an error message (using the actual
`error` string from the backend's `{"error": "..."}` body) on failure, and
an explicit empty-state message when a list comes back empty (e.g. "No
items registered yet").

### Architecture / Business rules

**27. How is "an item can't have two active pickups" enforced?**
`PickupService.requestPickup()` calls
`pickupRepository.existsByItem_IdAndStatusNot(itemId, PickupStatus.RECYCLED)`
before creating a new pickup; if true, it throws
`ActivePickupExistsException`, mapped to HTTP 409. A derived query, not a
loaded-then-filtered list, so the check doesn't require pulling every
pickup into memory.

**28. Why does `Pickup` own pickup status instead of `EwasteItem`?**
It used to be tracked on both, which meant two separate `setStatus` calls
had to be kept in sync on every update - an easy way to introduce a bug
the moment one call site was missed. Since "where is this item in the
collection lifecycle" is conceptually `Pickup`'s question, not
`EwasteItem`'s, the duplicate field was removed from `EwasteItem`.

**29. What are this project's actual limitations?**
No authentication (anyone can pick any demo user, including the recycler
role, from a dropdown); H2 loses all data on restart locally; reward
numbers are prototype assumptions, not real market prices; no pagination
on list endpoints; frontend UI was never visually verified in an actual
browser in this environment (only the build and the underlying HTTP calls
were verified).

**30. If you had more time, what would you improve first?**
Real authentication (see Q40 below); Bean Validation annotations
(`@NotBlank`, `@Positive`) instead of hand rolled checks; pagination on
`/api/items` and `/api/pickups`; controller-layer (`@WebMvcTest`) tests to
cover HTTP-level concerns the current service-level tests don't (status
codes, malformed JSON); a real migration tool (Flyway/Liquibase) instead
of `ddl-auto` for the PostgreSQL deployment path.

### Domain model additions: User, RecyclerCenter, pickup lifecycle, PostgreSQL

**31. Why did you introduce `User`?**
To make the domain model realistic: a real e-waste platform has people
using it - citizens registering devices, recyclers managing pickups - and
"who owns this item" / "who requested this pickup" are real questions
worth modeling. `User` is a plain entity (`id`, `name`, `email`, `role`)
with **no password field** - it models identity for domain purposes, not
for logging in. `EwasteItem.owner` and `Pickup.requestedBy` are both
optional `@ManyToOne User` references, set only when the frontend's
demo-mode switcher supplies a `userId`.

**32. Why are Citizen and Recycler separate roles instead of one generic
user?**
Because the two have genuinely different capabilities in this app -
citizens register items and request pickups; recyclers assign centers,
schedule pickups, and advance status. `UserRole` (`CITIZEN`/`RECYCLER`)
is a plain enum field on `User`. The frontend uses it to decide which
controls to show (e.g. `Pickups.jsx` only renders the assign/schedule/
advance controls when `currentUser.role === 'RECYCLER'`) - but this is a
**UI convenience, not an authorization check**. The backend does not
verify roles on any endpoint; a citizen's browser could call the assign
endpoint directly and it would succeed. That's a direct consequence of
having no authentication - see Q35.

**33. Why did you create `RecyclerCenter` instead of keeping the free-text
field?**
The free-text field let two pickups be "assigned" to two different
spellings of the same center, and gave the frontend nothing to build a
proper dropdown from. `RecyclerCenter` (`id`, `name`, `location`) is a
real entity now; `Pickup.recyclerCenter` is a `@ManyToOne` reference set
via `PUT /api/pickups/{id}/assign` with a `recyclerCenterId`. Deliberately
simple - no capacity, availability, or distance modeling, since none of
that was asked for.

**34. What are the database relationships in the final model?**
```
app_user (1) ---- (*) ewaste_item      (owner_id, nullable)
app_user (1) ---- (*) pickup           (requested_by_id, nullable)
ewaste_item (1) ---- (*) pickup        (item_id, not null)
recycler_center (1) ---- (*) pickup    (recycler_center_id, nullable)
```
Everything except `pickup.item_id` is nullable, because without a login
there's no guarantee a `userId` (or, before assignment, a
`recyclerCenterId`) is available at the time a row is created. See
`DATABASE.md` for the full column-level breakdown.

**35. Why did you not implement authentication?**
It was explicitly out of scope for this project - the goal was a
realistic *domain* model (users, roles, relationships) without the
considerable extra surface area of passwords, sessions/JWTs, and
authorization filters, which would have doubled the project's complexity
for a fresher-level portfolio piece. The `role` field and demo-mode
switcher demonstrate the *concept* of role-aware behavior on the frontend
without claiming the security guarantees real auth would provide -
nothing here should be described as "secure" in an interview.

**36. Why are pickup statuses restricted to specific transitions?**
Because the recycler-side workflow assumes pickups move through the
lifecycle in order - a real operator can't collect an item that was never
scheduled, and a status jumping backwards (e.g. `SCHEDULED -> ASSIGNED`)
would corrupt whatever the UI/operator believed had already happened.
`PickupService.VALID_NEXT_STATUS` is a single `Map<PickupStatus,
PickupStatus>` (an `EnumMap`) checked by every status-changing method
before it mutates anything - a plain map, not a state-machine framework,
since four fixed transitions don't need one.

**37. How does the pickup lifecycle work end to end?**
`PENDING` (created by `requestPickup`) `->` `ASSIGNED` (via `PUT
/assign`, which also sets the `RecyclerCenter`) `->` `SCHEDULED` (via
`PUT /schedule`, which also sets `pickupDate`/`pickupTime`) `->`
`COLLECTED` `->` `RECYCLED` (both via the generic `PUT /status`, since
neither needs extra data). Every step is validated against the same
transition map regardless of which endpoint triggers it - see `DATA_FLOW.md`.

**38. Why H2 locally?**
Zero setup - no install, no running service, works the moment you run the
app. The known tradeoff (`ddl-auto=create-drop` wipes data on every
restart) is acceptable for local development where nothing needs to
persist between runs.

**39. Why PostgreSQL for deployment?**
A real deployment needs data to survive a restart, which H2's in-memory
mode can't do. The switch requires no JPA/entity code changes - only
`application.properties` (no hardcoded driver class, so Spring Boot infers
H2 vs PostgreSQL from the JDBC URL) plus the `postgresql` runtime
dependency already on the classpath. Credentials are never hardcoded -
`SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` are read from the
environment, following Spring Boot's own naming convention.

**40. How would authentication be added in a future production version?**
`User` already models identity and role, so the domain modeling work is
largely done. The missing piece is: (1) a password/credential mechanism
(or an external identity provider), (2) a login endpoint issuing a
session or JWT, (3) a filter (e.g. Spring Security) that resolves the
authenticated `User` from each request instead of trusting a client-
supplied `userId` query param, and (4) authorization checks so only a
`RECYCLER` can call the assign/schedule/status endpoints, and a `CITIZEN`
can only see their own items/pickups (not just have the frontend hide
the option, as it does today). None of this exists yet - every current
endpoint trusts whatever `userId` it's given.
