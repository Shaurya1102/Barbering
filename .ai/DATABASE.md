# Barbering Database

## Accepted architecture

PostgreSQL is the production database (ADR-001). Spring Data JPA/Hibernate
maps application data; ordered Flyway migrations own the schema. Hibernate is
configured to validate mappings, not create or update tables. Credentials are
environment supplied:

- `BARBERING_DB_URL` (default `jdbc:postgresql://localhost:5432/barbering`)
- `BARBERING_DB_USER` (default `barbering`)
- `BARBERING_DB_PASSWORD`

Automated tests use H2 in PostgreSQL compatibility mode with the same Flyway
migrations. On 2026-10-10, PostgreSQL 18.6 was started locally; the existing
`barbering` database migrated from V1 to V3 and Hibernate validated the schema.
A separate temporary PostgreSQL database also passed a barber setup, discovery,
availability, customer booking, and barber confirmation flow; that database
was removed after verification. Flyway warned that PostgreSQL 18 is newer than
its currently tested maximum (17), although these migrations and startup
completed successfully.

## Implemented schema

- `V1__create_auth_schema.sql`: `users`, `user_password_credentials`, and
  reserved `user_auth_identities`.
- `V2__create_marketplace_schema.sql`: `barber_profiles`, `services`,
  `barber_services`, `working_hours`, `barber_breaks`, `barber_photos`,
  `appointments`, `reviews`, and `favourites`.
- `V3__align_integer_column_types.sql`: aligns three constrained small-number
  columns with their Java `int` mappings while preserving their data and
  constraints.
- `V4__barber_service_details.sql`: adds per-offering `name_override` and
  `description_override` columns to `barber_services`, keeping service edits
  local to one barber's offering while the shared catalog stays a source of
  defaults.
- `V5__google_places_discovery.sql`: adds `latitude`, `longitude`, and unique
  `google_place_id` to `barber_profiles`, and creates the `place_claims` table
  (`user_id`, `barber_profile_id`, unique `google_place_id`, `status`
  PENDING/APPROVED/REJECTED, `requested_at`, `reviewed_at`) with a profile
  index. Only place IDs and claim metadata are stored; full Google payloads
  stay external.

The migration files are the exact source of truth. They define foreign keys,
checks, unique constraints, and indexes. See `DATABASE_SCHEMA.md` for columns
and invariants.

## Appointment integrity

Booking is a transactional application operation. Before validating a slot,
`AppointmentService` obtains a pessimistic write lock on the target
`barber_profiles` row. Concurrent bookings for that barber serialize; the
later transaction checks pending/confirmed appointments again before it
inserts. The check uses half-open overlap semantics (`existing.start < new.end`
and `existing.end > new.start`). The lock also protects rescheduling. This
prevents double booking through the application API without a scheduler or
stored slot table.

Appointment price, service name, and duration are snapshots, so changing a
barber's current menu does not rewrite history. Disabled offerings are not
available for new bookings.

## Time handling

Working hours and recurring breaks use local `TIME` values. Each barber profile
stores a Java time-zone ID. Availability resolves local dates in that zone and
returns offset-qualified times; appointments persist as time-zone-aware
timestamps. Gaps in daylight-saving transitions produce no slot. The frontend
displays the barber's configured zone.

## Photos

PostgreSQL stores photo metadata and an opaque UUID storage key. A
`PhotoStorage` interface isolates binaries from the database. The current
local-development adapter stores validated JPEG/PNG/WebP files under the
configured `BARBERING_UPLOAD_DIR` (default `./uploads/barber-photos`), limited
to 5 MB. Public URLs use a photo database ID; clients never receive a
filesystem path. No cloud object-storage provider or credential is configured.

## Deferred schema

Notifications/reminders, shops, walk-in queues, payments, and persisted AI
generation records are not implemented. Do not add them until their features
are defined.
