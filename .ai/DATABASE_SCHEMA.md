# Implemented Barbering Schema

Flyway migrations under `backend/src/main/resources/db/migration/` own the
physical schema. This summary describes the current migrations; update the
migration and this document together.

## Identity — V1

- `users`: identity, unique email, display name, `CUSTOMER`/`BARBER` role,
  enabled flag, and timestamps.
- `user_password_credentials`: user-owned BCrypt hash and timestamps.
- `user_auth_identities`: unique provider/subject pairs, reserved for a future
  OAuth provider.

## Marketplace — V2

- `barber_profiles`: one profile per user; bio, city, region, street address,
  postal code, country, and `timezone_id` (default `UTC`).
- `services`: service catalog names and descriptions.
- `barber_services`: unique profile/service offering, nonnegative decimal
  price, 5–480 minute duration, enabled flag, and timestamps.
- `working_hours`: one row per profile and ISO day-of-week, local start/end,
  enabled flag; database check enforces day 1–7 and start before end.
- `barber_breaks`: recurring weekly local start/end, optional reason; multiple
  breaks per day are allowed, each with a valid day and range.
- `barber_photos`: opaque storage key, MIME type, size, alt text, and owner.
- `appointments`: customer, barber profile, offering, snapshot service name,
  price and duration, offset-aware start/end, status, optional customer note,
  and timestamps. Valid statuses are `PENDING`, `CONFIRMED`, `REJECTED`,
  `CANCELLED`, `COMPLETED`, and `NO_SHOW`.
- `reviews`: one per appointment, customer, barber, 1–5 rating, optional text,
  timestamp.
- `favourites`: customer/barber pair is unique.

## Barber-owned service details — V4

- `barber_services.name_override` and `description_override` hold edits local
  to one barber's offering. Existing rows keep null overrides and continue to
  use shared `services` catalog defaults. Appointment rows keep their existing
  service name, price, and duration snapshots after an offering is edited.

## Mapping alignment — V3

V3 changes `working_hours.day_of_week`, `barber_breaks.day_of_week`, and
`reviews.rating` from `SMALLINT` to `INTEGER` to match their Java `int`
entity properties. Values and range constraints are preserved.

All domain ownership references use foreign keys. Appointment search has
barber/start and customer/start indexes; ratings, city, and enabled offerings
have supporting indexes.

## Invariants

- A barber profile is unique per user; service offerings are unique per barber
  and catalog service.
- A review is unique per appointment and its rating is constrained to 1–5.
- A favourite pair is unique.
- New bookings require an enabled offering and pass the availability check
  inside a transaction while holding a pessimistic write lock on the barber
  profile row. That serializes competing requests for the same barber.
- Only pending/confirmed appointments block new intervals. Appointment
  intervals are half-open, so one appointment may start exactly when another
  ends.

Future notifications, shops, payments, queue records, and AI records are not
present in V1/V2/V3/V4.
