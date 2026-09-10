# OOP Concepts in ReLoop

All five concepts below are demonstrated with genuine reasons, not forced
in for the sake of ticking a box.

## 1. Encapsulation

**What it means**: an object's internal state is private; the outside
world interacts with it only through methods that can enforce rules.

**Where**: `EwasteItem` (`backend/.../model/EwasteItem.java`). All fields
are `private`. `setWeightKg()` throws `IllegalArgumentException` if given
a value `<= 0` — so it's structurally impossible to construct or mutate an
`EwasteItem` into an invalid weight state; every subclass constructor
routes through it via `super(...)`.

**Why**: the weight-must-be-positive rule needs to hold everywhere the
field is set, not just at the one call site the developer remembers to
check. Putting the check inside the setter makes it impossible to bypass.

**Interview explanation**: "I made `weightKg` private and only reachable
through a setter that validates it, so an invalid weight can't exist
anywhere in the object's lifetime, not just at creation."

**Likely question**: *"Why not just validate in the controller?"* — because
that would only catch the HTTP-request path; any other code constructing
an `EwasteItem` directly (as the unit tests do) would bypass it. Putting
the invariant on the object itself makes it apply universally.

## 2. Abstraction

**What it means**: expose *what* an object can do without exposing *how*.

**Where**: `EwasteItem` declares `calculateReward()`, `getItemType()`, and
`getPriorityWeight()` as `abstract` methods. `PickupService` and
`EwasteService` call these methods on an `EwasteItem` reference without
ever knowing (or needing to know) which concrete subclass they're holding.

**Why**: the service layer's logic ("compute this item's reward",
"find the highest-priority pending pickup") is the same regardless of
device type — only the *formula* differs, and that's each subclass's
concern, not the service's.

**Interview explanation**: "The service layer asks an `EwasteItem` for its
reward and priority without an `if (type == LAPTOP)` anywhere — it doesn't
need to know or care what concrete class it's holding."

**Likely question**: *"What's the difference between abstraction and
encapsulation here?"* — encapsulation hides *data* (the private fields);
abstraction hides *implementation* (how the reward number is actually
computed).

## 3. Inheritance

**What it means**: a subclass reuses a shared base class's state and
contract.

**Where**: `Laptop`, `MobilePhone`, `Battery`, `Accessory` all `extends
EwasteItem`, inheriting `id`, `weightKg`, `condition`, `location`,
`description`, and the `conditionMultiplier()` helper.

**Why**: every device type genuinely shares those five fields and the
same condition-scaling idea (working devices worth more than damaged
ones) — real shared state, not inheritance forced onto unrelated classes.

**Interview explanation**: "All four device types are-a `EwasteItem` —
they share identity, physical properties, and location — so common state
lives once in the base class."

**Likely question**: *"Why abstract class instead of an interface?"* —
because the subclasses share actual field state (`weightKg`, `condition`,
etc.) and a concrete helper method (`conditionMultiplier()`), not just a
method signature contract. An interface can't hold instance fields.

## 4. Polymorphism

**What it means**: calling the same method on different concrete types
produces different, type-appropriate behavior, selected at runtime.

**Where**: `calculateReward()` is overridden differently by all four
subclasses:
- `Laptop`: `weightKg * 40 * conditionMultiplier()` — laptops carry real
  recoverable material (aluminum, copper, rare-earths), so reward scales
  with weight and condition.
- `MobilePhone`: `80 * conditionMultiplier()` — phones don't vary enough
  in weight to make weight-based pricing meaningful, so it's a flat base
  adjusted by condition.
- `Battery`: `30 + (weightKg * 10)` — deliberately **ignores**
  `conditionMultiplier()`, because a battery needs the same hazardous
  handling whether it's "working" or "damaged"; it pays a flat
  hazard-handling fee plus a small per-kg component instead.
- `Accessory`: `20 * conditionMultiplier()` — negligible material value,
  small flat reward.

`EwasteService.registerItem()` calls none of these directly by name — it
holds an `EwasteItem` reference and calls `.calculateReward()`; the JVM
dispatches to the correct override based on the object's actual runtime
type.

**Why**: this is the strongest justification in the whole codebase for
*why* the hierarchy is abstract rather than one class with an `if/else`
chain — the four formulas are genuinely different in *shape*, not just
different constants plugged into one formula.

**Interview explanation**: "I never write `if (item instanceof Battery)`
anywhere in the service layer. I call `item.calculateReward()` and Java
picks the right implementation for whatever concrete object is there."

**Likely question**: *"Could you have done this with a single class and a
`switch` on an enum field instead?"* — yes, and for four near-identical
formulas that might even be simpler. The reason to prefer polymorphism
here is that `Battery` genuinely branches (it skips condition scaling
entirely) — that's a difference in *logic*, not just parameters, which is
exactly the case polymorphism is meant for.

## 5. Composition

**What it means**: an object is built *from* other objects ("has-a"),
rather than being a specialized version of one ("is-a").

**Where**: `Pickup` (`backend/.../model/Pickup.java`) holds a reference to
an `EwasteItem` via `@ManyToOne`. `Pickup` is **not** a subclass of
`EwasteItem`.

**Why**: a pickup request and a physical device have different lifecycles
owned by different concerns — deleting a pickup record must never delete
the device, and conceptually an item could have more than one pickup over
its lifetime (if a first pickup falls through). "A pickup *has* an item"
is true; "a pickup *is* an item" is not — so composition is the correct
relationship, and forcing inheritance here would be wrong.

**Interview explanation**: "`Pickup` doesn't extend `EwasteItem` because a
pickup isn't a *kind of* device — it's a separate record that *references*
one. That's composition, not inheritance."

**Likely question**: *"How do you decide is-a vs has-a?"* — ask whether
every instance of the subclass really is a specialization of the
superclass's concept. A `Laptop` really is a kind of `EwasteItem`. A
`Pickup` is not a kind of `EwasteItem` — it's a separate concept that owns
a reference to one, which is why it uses `@ManyToOne` composition instead.

## Composition, extended: `User` and `RecyclerCenter`

`EwasteItem.owner` and `Pickup.requestedBy` are both `@ManyToOne User`
references - more composition, for the same reason as `Pickup`/
`EwasteItem`: a citizen *has* devices and pickups, a device or pickup
isn't *a kind of* citizen. Likewise `Pickup.recyclerCenter` is a
`@ManyToOne RecyclerCenter` - a pickup *has* an assigned center, it isn't
a specialization of one.

**Why `User` and `RecyclerCenter` are NOT part of the `EwasteItem`
hierarchy**: neither shares `EwasteItem`'s identity ("a device being
recycled, with a reward"). Forcing them into the hierarchy just to reuse
`id`/fields would be exactly the "inheritance forced into unrelated
classes" this project explicitly avoids. They're separate, unrelated
entities connected only by association (foreign keys), which is the
correct relationship for "a citizen owns many devices" or "a center
handles many pickups."

**Interview explanation**: "Adding `User` didn't touch the `EwasteItem`
hierarchy at all - it's a separate entity that `EwasteItem` and `Pickup`
each hold an optional reference to. That's composition again, just one
more example of it, not a reason to redesign the hierarchy."

## Conceptual split worth remembering for interviews

- **`EwasteItem` answers**: "What is being recycled?" (device identity,
  weight, condition, reward.)
- **`Pickup` answers**: "Where is this item in the collection lifecycle?"
  (status, priority, recycler assignment.)

Pickup lifecycle status used to also live on `EwasteItem` (a duplicated
field), which meant two update call sites had to be kept in sync — a bug
waiting to happen. It was removed from `EwasteItem` during a later cleanup
pass so `Pickup` is now the single owner of that state.
