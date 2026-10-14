# Barbering --- Architecture

## Current State

The repository now contains a session-authenticated marketplace foundation:
Next.js frontend, Spring Boot REST API, PostgreSQL/Flyway schema, public
barber discovery, profiles, services, weekly availability configuration,
availability calculation, appointment lifecycle, reviews, favourites, local
photo storage, and data-backed dashboards. See `CODEBASE_MAP.md` for the
current source inventory.

Google OAuth, password recovery/email verification, notification delivery,
PostgreSQL runtime verification, and a connected AI provider are not present.

## Approved Target Architecture

``` text
                     Browser
                        │
                        ▼
                 Next.js Frontend
                        │
                 HTTPS / HTTP API
                        │
                        ▼
               Spring Boot Backend
                        │
        ┌───────────────┼────────────────┐
        │               │                │
   Spring Security   Business        REST API
        │             Services           │
        │               │                │
        └───────────────┼────────────────┘
                        │
                  Spring Data JPA
                        │
                        ▼
                   PostgreSQL
```

Authentication:

``` text
Browser
  │
  │ HTTP-only session cookie
  ▼
Spring Security
  │
  ├── email/password
  └── Google OAuth
  │
  ▼
Application User
```

Images:

``` text
Frontend
  │
  ▼
Backend authorization/upload flow
  │
  ├── metadata → PostgreSQL
  └── binary → object storage
```

## Domain Boundaries

-   Identity/authentication
-   Customer
-   Barber
-   Service
-   Availability
-   Appointment
-   Review
-   Favourite
-   Search/discovery
-   AI
-   Notification
-   Future payment

## Public Discovery

Anonymous discovery should work independently of authentication.

## Booking

Booking is authenticated.

Availability is derived from: working hours + breaks + existing
appointments + service duration + booking rules.

The backend must protect against concurrent double booking.

## Roles

Minimum roles: - CUSTOMER - BARBER

Design authorization so ADMIN can be introduced later.

## Database

PostgreSQL is the approved relational database.

Use JPA/Hibernate and Flyway unless an ADR changes the decision.

## Images

The storage interface keeps the database independent of image binaries. The
current local-development implementation validates and stores barber photos
on disk; PostgreSQL stores metadata and opaque keys. A cloud object-storage
provider has not been configured.

## Payments

Not currently implemented. Do not add payment infrastructure unless
requested.

## Shop Model

The current product is barber-centric. Keep the model extensible for a
future shop containing multiple barbers, but do not introduce shop
entities without a concrete feature requirement.

## Current Booking Concurrency

Appointment creation and rescheduling run transactionally and lock the
barber profile row with `PESSIMISTIC_WRITE` before rechecking overlaps. This
serializes application API requests for one barber and prevents concurrent
double booking. Appointment slots remain calculated on request rather than
materialized.

## AI Provider Boundary

Authenticated AI routes accept typed request DTOs through an `AiProvider`
interface. The default `UnavailableAiProvider` returns 503; it sends no
customer information to an external service and does not simulate results.
