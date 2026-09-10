# API Reference

Base URL (dev): `http://localhost:8080`

All request/response bodies are JSON. Errors are returned as
`{"error": "<message>"}` with an appropriate HTTP status (see
`GlobalExceptionHandler`).

**No endpoint here requires authentication.** `userId` fields are plain,
unverified numbers the frontend sends because its demo-mode user switcher
picked a user - there is no session, token, or password anywhere in this
API. See `README.md` "Limitations".

---

### `POST /api/items`

Register a new e-waste item.

**Request body**
```json
{
  "itemType": "LAPTOP",       // LAPTOP | MOBILE_PHONE | BATTERY | ACCESSORY
  "weightKg": 2.5,
  "condition": "WORKING",     // WORKING | PARTIALLY_WORKING | DAMAGED
  "location": "College Hostel",
  "description": "Dell Inspiron, 2020 model",   // optional
  "userId": 1                                    // optional - links this item to a citizen
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "itemType": "Laptop",
  "weightKg": 2.5,
  "condition": "WORKING",
  "location": "College Hostel",
  "description": "Dell Inspiron, 2020 model",
  "estimatedReward": 100.0,
  "priorityWeight": 2,
  "owner": { "id": 1, "name": "Demo Citizen", "email": "citizen@example.com", "role": "CITIZEN" }
}
```
`owner` is `null` if no `userId` was supplied.

**Errors**
- `400` - unknown `itemType`, unknown `condition`, blank `location`, or
  `weightKg <= 0`.
- `404` - `userId` was supplied but doesn't exist.

---

### `GET /api/items?userId={id}`

List items. Without `userId`, returns every item (used by the recycler
side of the app, and internally by `/api/impact`). With `userId`, returns
only that citizen's own registered items (backs the "My Items" page).

**Response** `200 OK` - JSON array of items.

---

### `GET /api/items/{id}`

Fetch a single item.

**Response** `200 OK` - single item object.
**Errors** `404` - no item with that ID.

---

### `GET /api/items/recent?limit=5`

Most recently registered items, newest first (used by the Dashboard's
"Recent Activity" section). `limit` is optional, defaults to 5.

**Response** `200 OK` - JSON array, length `<= limit`.

---

### `POST /api/pickups`

Request a pickup for an existing item.

**Request body**
```json
{ "itemId": 1, "pickupLocation": "College Hostel", "userId": 1 }
```
`userId` is optional - links this pickup to the citizen who requested it.

**Response** `201 Created`
```json
{
  "id": 3,
  "item": { "...": "full EwasteItem object" },
  "requestedBy": { "id": 1, "name": "Demo Citizen", "...": "..." },
  "status": "PENDING",
  "priorityWeight": 2,
  "recyclerCenter": null,
  "pickupLocation": "College Hostel",
  "pickupDate": null,
  "pickupTime": null
}
```

**Errors**
- `400` - blank `pickupLocation`.
- `404` - `itemId` (or `userId`, if supplied) doesn't exist.
- `409` - the item already has a non-`RECYCLED` pickup in progress.

---

### `GET /api/pickups?userId={id}`

List pickups. Without `userId`, returns every pickup (the recycler
operator view). With `userId`, returns only pickups that citizen
requested (backs their "My Pickups" view on the Pickups page).

**Response** `200 OK` - JSON array of pickups.

---

### `GET /api/pickups/{id}`

Fetch a single pickup.

**Response** `200 OK` - single pickup object.
**Errors** `404` - no pickup with that ID.

---

### `GET /api/pickups/pending`

Pending pickups ordered by **hazard priority**
(Battery > Laptop > Mobile Phone > Accessory), not arrival order -
backed by a `PriorityQueue` (see `DATA_FLOW.md` for the honest complexity
discussion).

**Response** `200 OK` - JSON array of pickups, in priority order.

---

### `PUT /api/pickups/{id}/assign`

**PENDING → ASSIGNED.** Assigns a recycler center to the pickup.

**Request body**
```json
{ "recyclerCenterId": 1 }
```

**Response** `200 OK` - updated pickup object, `recyclerCenter` now populated.

**Errors**
- `404` - no pickup, or no recycler center, with that ID.
- `409` - the pickup isn't currently `PENDING` (see "Valid transitions" below).

---

### `PUT /api/pickups/{id}/schedule`

**ASSIGNED → SCHEDULED.** Records when the recycler will collect the item.

**Request body**
```json
{ "pickupDate": "2026-09-15", "pickupTime": "14:30:00" }
```

**Response** `200 OK` - updated pickup object, `pickupDate`/`pickupTime` set,
`status` now `SCHEDULED`.

**Errors**
- `400` - `pickupDate` or `pickupTime` missing.
- `404` - no pickup with that ID.
- `409` - the pickup isn't currently `ASSIGNED`.

---

### `PUT /api/pickups/{id}/status`

Advances a pickup to the next status in its lifecycle: **SCHEDULED →
COLLECTED**, or **COLLECTED → RECYCLED**. (PENDING → ASSIGNED and
ASSIGNED → SCHEDULED go through the two dedicated endpoints above instead,
since they each need extra data - a recycler center, a date/time - that
this generic endpoint doesn't take.)

**Request body**
```json
{ "status": "COLLECTED" }
```

**Response** `200 OK` - updated pickup object.

**Errors**
- `400` - `status` isn't one of PENDING/ASSIGNED/SCHEDULED/COLLECTED/RECYCLED.
- `404` - no pickup with that ID.
- `409` - the requested status isn't the valid next step from the
  pickup's current status (skipping a step or moving backwards). See
  "Valid transitions" below.

#### Valid transitions

```
PENDING → ASSIGNED → SCHEDULED → COLLECTED → RECYCLED
```

Every other change - skipping a step (`PENDING → COLLECTED`), moving
backwards (`SCHEDULED → ASSIGNED`), or changing a terminal `RECYCLED`
pickup - is rejected with `409 Conflict` regardless of which endpoint is
used, because all three status-changing methods
(`assignRecycler`/`scheduleDatetime`/`updateStatus`) check the same single
transition table in `PickupService`.

---

### `GET /api/impact`

Aggregate dashboard/impact statistics across every item and pickup
(not scoped to a single user - this is a whole-system summary).

**Response** `200 OK`
```json
{
  "totalItemsRegistered": 4,
  "activePickups": 3,
  "recycledCount": 1,
  "totalEcoPoints": 208.0,
  "estimatedEwasteDivertedKg": 4.0,
  "weightByTypeKg": { "Laptop": 2.5, "Battery": 1.0, "Mobile Phone": 0.3, "Accessory": 0.2 }
}
```

All figures here are **estimates** derived from registered weight/reward
data, not measured environmental science - the frontend labels them as
such wherever they're displayed.

---

### `GET /api/users`

List every user (citizens and recyclers). Backs the frontend's demo-mode
user switcher - there is no login, so the frontend needs a way to see who
it can "act as."

**Response** `200 OK`
```json
[
  { "id": 1, "name": "Demo Citizen", "email": "citizen@example.com", "role": "CITIZEN" },
  { "id": 2, "name": "Demo Recycler", "email": "recycler@example.com", "role": "RECYCLER" }
]
```

### `POST /api/users`

Create a user. Minimal - no password, no validation beyond what JPA
enforces. Mainly useful for adding more demo users than the two seeded at
startup.

**Request body**
```json
{ "name": "Another Citizen", "email": "another@example.com", "role": "CITIZEN" }
```

**Response** `201 Created` - the created user, with its generated `id`.

---

### `GET /api/recycler-centers`

List every recycler center. Backs the Pickups page's "assign" dropdown.

**Response** `200 OK`
```json
[
  { "id": 1, "name": "GreenRecycle Center", "location": "College Hostel" },
  { "id": 2, "name": "EcoWaste Hub", "location": "City Center" }
]
```

### `POST /api/recycler-centers`

Create a recycler center.

**Request body**
```json
{ "name": "New Center", "location": "Somewhere" }
```

**Response** `201 Created` - the created center, with its generated `id`.
