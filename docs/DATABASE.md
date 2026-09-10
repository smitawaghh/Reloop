# Database

## Why H2 locally / PostgreSQL for deployment

H2 is an in-memory database that needs zero setup - no install, no
running service, no credentials to manage. That's the right tradeoff for
local development: anyone cloning the repo can run it immediately. **The
tradeoff is that all data is lost every time the backend restarts**
(`spring.jpa.hibernate.ddl-auto=create-drop` drops and recreates the
schema on every startup/shutdown).

For deployment, the same codebase talks to PostgreSQL instead - no JPA or
entity code changes needed, only configuration:

- The `postgresql` JDBC driver is on the classpath alongside `h2` (both
  `runtime` scope in `pom.xml`).
- `application.properties` deliberately does **not** hardcode a
  `driverClassName` - Spring Boot infers the correct driver from the JDBC
  URL's prefix (`jdbc:h2:` vs `jdbc:postgresql:`), so the same properties
  file works for either database.
- Connection details are read from the standard Spring Boot environment
  variables - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  `SPRING_DATASOURCE_PASSWORD` - which override the H2 defaults in
  `application.properties` without editing any file. No credentials are
  hardcoded anywhere in the repository.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update` (or `validate`) should be set in
  a deployment where data needs to survive a restart - the local default
  (`create-drop`) is intentionally destructive.

No migration tool (Flyway/Liquibase) was introduced - out of scope for
this pass, and `ddl-auto` is an acceptable substitute at this project's
scale.

## Entities

### `app_user`

Backs `User`. Named `app_user` rather than `user` because `user` is a
reserved keyword in both H2 and PostgreSQL SQL grammar.

| Column  | Type                          | Notes |
|---------|-------------------------------|-------|
| `id`    | `bigint`, identity             | primary key |
| `name`  | `varchar`                      | |
| `email` | `varchar`                      | |
| `role`  | enum string (CITIZEN/RECYCLER) | no password column - see `PROJECT_OVERVIEW.md`/`INTERVIEW_QA.md` for why |

### `recycler_center`

Backs `RecyclerCenter`.

| Column     | Type              | Notes |
|------------|-------------------|-------|
| `id`       | `bigint`, identity | primary key |
| `name`     | `varchar`          | |
| `location` | `varchar`          | |

### `ewaste_item`

Backs the abstract `EwasteItem` class and its four subclasses, using
**single-table inheritance** (`@Inheritance(strategy = SINGLE_TABLE)`):
all four subclasses share one table, distinguished by a discriminator
column.

| Column       | Type                                          | Notes |
|--------------|-----------------------------------------------|-------|
| `id`         | `bigint`, identity                            | primary key |
| `item_type`  | `varchar`, constrained to LAPTOP/MOBILE_PHONE/BATTERY/ACCESSORY | discriminator column - Hibernate sets this automatically based on which subclass you saved |
| `weight_kg`  | `float`, not null                              | validated `> 0` before it ever reaches the database |
| `condition`  | enum string (WORKING/PARTIALLY_WORKING/DAMAGED) | |
| `location`   | `varchar`                                      | validated non-blank in the service layer |
| `description`| `varchar`, nullable                            | optional |
| `owner_id`   | `bigint`, nullable, FK -> `app_user.id`         | the citizen who registered this item, if a demo user was selected |

Single-table inheritance was chosen over joined-table or table-per-class
because all four subclasses share the exact same columns - there's no
subclass-specific field that would otherwise force a join. It's the
simplest option that fits.

### `pickup`

Backs the `Pickup` entity - one row per pickup request.

| Column              | Type                                    | Notes |
|---------------------|------------------------------------------|-------|
| `id`                | `bigint`, identity                        | primary key |
| `item_id`           | `bigint`, not null, FK -> `ewaste_item.id` | `@ManyToOne` - composition, see `OOP_CONCEPTS.md` |
| `requested_by_id`   | `bigint`, nullable, FK -> `app_user.id`    | the citizen who requested this pickup, if a demo user was selected |
| `status`            | enum string (PENDING/ASSIGNED/SCHEDULED/COLLECTED/RECYCLED) | sole owner of pickup lifecycle |
| `priority_weight`   | `int`, not null                           | copied from `item.getPriorityWeight()` at creation time, so priority ordering doesn't need to re-load every item |
| `recycler_center_id`| `bigint`, nullable, FK -> `recycler_center.id` | set when a recycler assigns a center (`PUT /assign`) |
| `pickup_location`   | `varchar`                                  | validated non-blank |
| `pickup_date`       | `date`, nullable                          | set together with `pickup_time` when a recycler schedules the pickup (`PUT /schedule`) |
| `pickup_time`       | `time`, nullable                          | |

## Relationships

```
app_user (1) ---- (*) ewaste_item      (owner_id)
app_user (1) ---- (*) pickup           (requested_by_id)
ewaste_item (1) ---- (*) pickup        (item_id)
recycler_center (1) ---- (*) pickup    (recycler_center_id)
```

All four foreign keys except `pickup.item_id` are **nullable** - there's
no login, so an item/pickup only gets an owner/requester when the
frontend's demo-mode user switcher happened to have a `userId` to send,
and a pickup only gets a recycler center once one is actually assigned.

## How JPA connects Java objects to rows

`EwasteItemRepository`, `PickupRepository`, `UserRepository`, and
`RecyclerCenterRepository` are plain interfaces extending
`JpaRepository<T, Long>` - no implementation code was written. At
startup, Spring Data JPA generates a proxy implementation from the
interface signature: `save()`, `findById()`, `findAll()` come from
`JpaRepository` itself, while methods like `findByOwner_Id(Long)`,
`findByRequestedBy_Id(Long)`, and `existsByItem_IdAndStatusNot(Long,
PickupStatus)` are **derived queries** - Spring parses the method name
(including the `_Id` traversal into a related entity's `id` field) and
builds the corresponding JPQL/SQL automatically. No `@Query` annotation or
hand-written SQL was needed anywhere in this project.

## Persistence limitation (local dev only)

Because `spring.jpa.hibernate.ddl-auto=create-drop` recreates the schema
every startup, **restarting the backend locally deletes all registered
items, pickups, users beyond the two seeded ones, and recycler centers
beyond the two seeded ones**. This is intentional for local development
(no migration tooling needed), and is exactly what the `SPRING_DATASOURCE_*`
+ `SPRING_JPA_HIBERNATE_DDL_AUTO` environment variables above are for
overriding in a real deployment.
