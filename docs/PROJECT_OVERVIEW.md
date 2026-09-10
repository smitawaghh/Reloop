# Project Overview

## Problem

People accumulate unwanted electronics — old laptops, phones, batteries,
cables — with no easy way to register them for responsible recycling or
arrange a pickup. E-waste often ends up in general trash, where batteries
in particular are a genuine hazard (leaking chemicals, fire risk in waste
streams).

## Motivation

This is a university fresher portfolio project. The goal is a small,
fully-explainable system that still solves a real problem — not a toy
CRUD app, but also not an enterprise system. Every class in this codebase
should be explainable in an interview.

## Users

Modeled by the `User` entity with a `role` of `CITIZEN` or `RECYCLER`:

- **Citizen**: registers a device, requests its pickup, tracks their own
  pickups' status, sees estimated eco-points and impact.
- **Recycler**: views pending pickups (ordered by hazard priority),
  assigns a `RecyclerCenter`, schedules a pickup date/time, advances
  pickup status through a validated lifecycle.

**There is no login system.** The frontend has a "DEMO" dropdown in the
nav bar that switches between the seeded Citizen and Recycler users - this
drives which pages/controls are shown, but the backend does not verify
that whoever is making a request "really is" that user. Adding real
authentication was explicitly out of scope for this project - see
`docs/INTERVIEW_QA.md` Q35/Q40 for what a production version would need.

## Solution

ReLoop models e-waste as a small class hierarchy (`EwasteItem` and four
subclasses), computes an estimated reward per device type, and tracks each
device's collection lifecycle through a separate `Pickup` entity, which
can optionally be linked to the `User` who requested it and the
`RecyclerCenter` assigned to handle it. A REST API backed by Spring Boot
(H2 locally, PostgreSQL for deployment) serves a React frontend.

## Features

- Register e-waste (Laptop / Mobile Phone / Battery / Accessory) with
  weight, condition, location, description; optionally linked to the
  registering citizen.
- Estimated eco-points reward, calculated differently per device type.
- Request a pickup for a registered item (one active pickup per item at a
  time).
- Pickup lifecycle with **validated transitions**: PENDING → ASSIGNED →
  SCHEDULED → COLLECTED → RECYCLED. Skipping a step or moving backwards is
  rejected (HTTP 409).
- Recycler view: pending pickups ordered by hazard priority
  (Battery > Laptop > Mobile Phone > Accessory), assign a real
  `RecyclerCenter`, schedule a pickup date/time, advance status.
- Dashboard and Impact pages showing aggregate, clearly-labeled *estimated*
  statistics.

## Technology

- **Frontend**: React + Vite, plain CSS (no UI framework, no router
  library — five pages are handled by a single piece of state in
  `App.jsx`).
- **Backend**: Java, Spring Boot (Web + Data JPA), Maven.
- **Database**: H2 in-memory locally; PostgreSQL for deployment (same JPA
  code — see `DATABASE.md`).
- **Communication**: REST/JSON.

## Limitations

- **No authentication** — anyone can switch to the Recycler demo user via
  the nav bar dropdown and use its controls; the backend trusts whatever
  `userId` a request supplies. This was explicitly out of scope; a real
  deployment would need at least a simple login plus authorization checks
  (see `docs/INTERVIEW_QA.md` Q40).
- **H2 is in-memory locally** — all data is lost on backend restart there.
  PostgreSQL (used for deployment) doesn't have this problem.
- **Reward values are prototype assumptions**, not real market recycling
  prices or scientific environmental figures — documented explicitly
  wherever they're shown.
- **No pagination** on list endpoints — acceptable at demo scale (tens of
  items), would need addressing at real scale.
- **No migration tool** (Flyway/Liquibase) — `ddl-auto` only, sufficient
  for this project's scale but not for a schema that needs to evolve
  safely over time with existing production data.
