# Data Flow

## Register E-Waste

```
React form (RegisterEwaste.jsx)
   -> api.registerItem({..., userId: currentUser?.id})    [services/api.js]
   -> POST /api/items                                      [HTTP]
   -> EwasteController.registerItem(EwasteItemRequest)      [controller]
   -> EwasteService.registerItem(request)                   [service]
        - parseCondition(): validates condition string
        - requireNonBlank(): validates location
        - switch on itemType -> new Laptop/MobilePhone/Battery/Accessory
          (constructor validates weight > 0 via setWeightKg)
        - if userId was supplied: look up the User (404 if missing) and
          call item.setOwner(user)
   -> itemRepository.save(item)                             [repository]
   -> Hibernate INSERT into ewaste_item                      [H2/PostgreSQL]
   -> saved entity (with computed estimatedReward, and owner if linked)
      serialized to JSON
   -> React shows success screen: item ID, estimated reward
```

If `itemType` is unrecognized, `condition` is unrecognized, `location` is
blank, `weightKg <= 0`, or a supplied `userId` doesn't exist, the service
throws (`InvalidItemException`, `IllegalArgumentException`, or
`ResourceNotFoundException`), `GlobalExceptionHandler` turns that into a
400/404 response with `{"error": "..."}`, and the React form displays it.

**Who is "the current user"?** There's no login. `RegisterEwaste.jsx`
receives `currentUser` as a prop from `App.jsx`, which loaded the list of
demo users from `GET /api/users` once at startup and lets a nav-bar
dropdown switch between them. `userId` is just a plain number sent along
with the request - nothing about it is verified as "really" belonging to
whoever is sitting at the browser.

## Request Pickup

```
React (MyItems.jsx "Request Pickup" button)
   -> api.requestPickup(itemId, pickupLocation, currentUser?.id)
   -> POST /api/pickups
   -> PickupController.requestPickup(PickupRequest)
   -> PickupService.requestPickup(itemId, pickupLocation, userId)
        - rejects blank pickupLocation
        - ewasteService.getItemById(itemId) - 404 if missing
        - pickupRepository.existsByItem_IdAndStatusNot(itemId, RECYCLED)
          - if true, throws ActivePickupExistsException -> HTTP 409
        - new Pickup(item, pickupLocation) - copies item.getPriorityWeight()
          into the Pickup so priority ordering doesn't need to reload items
        - if userId was supplied: look up the User (404 if missing) and
          call pickup.setRequestedBy(user)
   -> pickupRepository.save(pickup)
   -> Hibernate INSERT into pickup
   -> React refetches items+pickups so the item's badge updates to PENDING
```

## Recycler priority view

```
GET /api/pickups/pending
   -> PickupController.getPendingByPriority()
   -> PickupService.getPendingPickupsByPriority()
        - pickupRepository.findByStatus(PENDING)   [List, arrival order]
        - PriorityQueue<Pickup> ordered by priorityWeight, all elements added
        - drained fully into a List (see DATABASE.md / inline comments for
          the honest complexity note - this is NOT asymptotically faster
          than sorting when the full list is needed every time)
   -> JSON array, already in Battery -> Laptop -> MobilePhone -> Accessory
      order regardless of the order pickups were requested in
   -> React (Pickups.jsx) renders it directly
```

## Assign, schedule, and advance a pickup (recycler operator flow)

Each step is guarded by the same transition table
(`PickupService.VALID_NEXT_STATUS`), regardless of which endpoint
triggers it:

```
PENDING
  -> PUT /api/pickups/{id}/assign  { recyclerCenterId }
     PickupService.assignRecycler():
       - validateTransition(current, ASSIGNED) - must currently be PENDING
       - recyclerCenterRepository.findById(recyclerCenterId) - 404 if missing
       - pickup.setRecyclerCenter(center); pickup.setStatus(ASSIGNED)
  -> ASSIGNED

ASSIGNED
  -> PUT /api/pickups/{id}/schedule  { pickupDate, pickupTime }
     PickupService.scheduleDatetime():
       - rejects a missing date or time (400)
       - validateTransition(current, SCHEDULED) - must currently be ASSIGNED
       - pickup.setPickupDate(...); pickup.setPickupTime(...); status = SCHEDULED
  -> SCHEDULED

SCHEDULED
  -> PUT /api/pickups/{id}/status  { "status": "COLLECTED" }
     PickupService.updateStatus():
       - validateTransition(SCHEDULED, COLLECTED) - allowed
  -> COLLECTED

COLLECTED
  -> PUT /api/pickups/{id}/status  { "status": "RECYCLED" }
     PickupService.updateStatus():
       - validateTransition(COLLECTED, RECYCLED) - allowed
  -> RECYCLED   (terminal - no further transition is valid from here)
```

Any attempt to skip a step (e.g. `PENDING -> COLLECTED` via the generic
status endpoint) or move backwards (`SCHEDULED -> ASSIGNED`) fails
`validateTransition()`, which throws
`InvalidPickupStatusTransitionException` -> HTTP 409, before any field is
mutated or saved.

## Dashboard / Impact aggregation

```
GET /api/impact
   -> ImpactController.getImpactStats()
   -> ImpactService.getStats()
        - loads all items + all pickups (system-wide, not scoped to a user)
        - sums weight and reward across items (calls calculateReward()
          polymorphically on each one)
        - HashMap<String, Double> weightByType: tallies weight per
          item.getItemType() - O(1) average get+put per item
        - counts recycled vs. active pickups via stream filters
   -> Map<String, Object> serialized directly to JSON (no extra DTO -
      this endpoint's whole job is producing exactly this shape)
   -> React (Dashboard.jsx, Impact.jsx) renders StatCards + a CSS bar chart
```
