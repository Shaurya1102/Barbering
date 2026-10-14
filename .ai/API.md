# Barbering API

Spring Boot owns the API and security boundary. JSON endpoints use the shared
`ApiError` shape: `{ "error": string, "details"?: string[] }`. Mutating
requests require the readable `XSRF-TOKEN` cookie echoed in the
`X-XSRF-TOKEN` header. The frontend API client bootstraps it with
`GET /auth/csrf` and includes the backend session cookie on every request.

## Authentication and profile

| Method and path | Access | Behavior |
|---|---|---|
| `POST /auth/signup` | Public + CSRF | `{name,email,password,role?}`. Creates a CUSTOMER by default or a BARBER with an empty barber profile. Returns 201 + `UserDto` and starts a session. |
| `POST /auth/login` | Public + CSRF | `{email,password}`. Returns `UserDto`; invalid email/password share a generic 401. |
| `GET /auth/me` | Authenticated | Returns the current persisted `UserDto`. |
| `POST /auth/logout` | Authenticated + CSRF | Invalidates the server session. |
| `GET /auth/csrf` | Public | Mints/returns the double-submit CSRF token and cookie. |
| `GET /profile/me` | Authenticated | Returns the signed-in user's profile. |
| `PUT /profile/me` | Authenticated + CSRF | `{displayName}` updates only the signed-in user's display name. |

`UserDto` is `{id,email,displayName,role}`. Roles are `CUSTOMER` and `BARBER`.
Password hashes and session identifiers are never returned.

## Public discovery

| Method and path | Behavior |
|---|---|
| `GET /barbers?query=&location=&service=&page=0&size=12` | Database-backed, paginated barber search. Query matches barber name, bio, and enabled service names; location matches street, city, region, postal code, and country. Each card includes name, location, rating, review count, bio, and photo links. `size` is capped at 50. |
| `GET /barbers/{barberId}` | Public barber profile and rating summary. |
| `GET /barbers/{barberId}/services` | Enabled offerings only, with service name, description, price, and duration. |
| `GET /barbers/{barberId}/reviews` | Public reviews for completed appointments. |
| `GET /barbers/{barberId}/availability?serviceId={offeringId}&date=YYYY-MM-DD` | Available slots derived from the barber's local working hours, recurring breaks, enabled service duration, existing pending/confirmed appointments, and past-time rules. Times include offsets; the response names the barber's time zone. Booking dates are limited to 90 days ahead. |
| `GET /barber-photos/{photoId}/content` | Streams a stored image by opaque database ID; filesystem paths are not accepted. |

`BarberDto` includes `id`, display name, bio, address/location fields,
`timezoneId`, `averageRating`, `reviewCount`, and photo metadata/URLs.

## Barber management

All `/barber/me/**` endpoints require `ROLE_BARBER`; services resolve the
profile from the authenticated user and verify ownership for resource IDs.

| Method and path | Behavior |
|---|---|
| `GET /barber/me/profile`, `PUT /barber/me/profile` | Read/update bio, address/location, and IANA time-zone ID. |
| `GET /barber/me/services`, `POST /barber/me/services` | List offerings (including disabled) or create an offering with `{name,description,priceAmount,durationMinutes}`. |
| `PUT /barber/me/services/{offeringId}` | Set `{name,description,priceAmount,durationMinutes,enabled}`. Name and description overrides are scoped to the barber's offering. Duration is 5–480 minutes; price cannot be negative. |
| `DELETE /barber/me/services/{offeringId}` | Disables the offering so history remains intact and it cannot be booked. |
| `GET /barber/me/working-hours`, `PUT /barber/me/working-hours` | Read or replace the weekly schedule. Each item is `{dayOfWeek,startTime,endTime,enabled}`; one row per day, local wall-clock time. |
| `GET /barber/me/breaks`, `POST /barber/me/breaks` | Read or add a weekly recurring break `{dayOfWeek,startTime,endTime,reason}`. Breaks must fit inside enabled working hours and cannot overlap. |
| `DELETE /barber/me/breaks/{breakId}` | Remove an owned recurring break. |
| `POST /barber/me/photos` | Multipart `file` and optional `altText`. Local development accepts valid JPEG/PNG/WebP up to 5 MB. |
| `DELETE /barber/me/photos/{photoId}` | Delete an owned photo. |
| `GET /barber/me/appointments` | Barber's appointment list. |
| `PATCH /barber/me/appointments/{appointmentId}/status` | `{status}` transitions PENDING→CONFIRMED/REJECTED or CONFIRMED→COMPLETED/NO_SHOW, with server-side ownership and time checks. |
| `GET /dashboard/barber` | Real pending/today/upcoming appointment data and lifetime completed revenue. |

## Customer actions

All `/appointments/**` and `/favourites/**` endpoints require `ROLE_CUSTOMER`.

| Method and path | Behavior |
|---|---|
| `GET /appointments/mine` | Customer's appointment history. |
| `POST /appointments` | `{barberId,serviceId,startAt,customerNote?}` creates a PENDING request after the backend revalidates the slot. |
| `PATCH /appointments/{appointmentId}/cancel` | Cancels an owned PENDING/CONFIRMED appointment when at least 24 hours remain. |
| `PATCH /appointments/{appointmentId}/reschedule` | `{startAt,customerNote?}` revalidates a new slot and resets the request to PENDING. |
| `POST /appointments/{appointmentId}/reviews` | `{rating,reviewText?}`. Only the appointment's customer may review a completed appointment; one review per appointment. |
| `GET /favourites` | Lists the customer's saved barbers. |
| `PUT /favourites/{barberId}` | Add a favourite; repeated adds are idempotent. |
| `DELETE /favourites/{barberId}` | Remove a favourite. |
| `GET /dashboard/client` | Real upcoming, pending, past appointments and favourites. |

## AI boundary

`POST /ai/recommendations` and `POST /ai/try-on` require an authenticated
session and CSRF. They validate typed request DTOs. Until an AI provider is
configured, they return 503 and do not send or process customer data. No
successful generation is simulated.

## Errors and security

Status codes: 400 validation/rule input, 401 unauthenticated or invalid
credentials, 403 insufficient role, 404 missing/foreign resource, 409
booking/state/uniqueness conflict, 500 unexpected failure, and 503 provider
unavailable. Cookie-authenticated writes remain CSRF protected. CORS uses one
explicit configured frontend origin (`BARBERING_FRONTEND_ORIGIN`); credentialed
wildcard origins are not enabled.
