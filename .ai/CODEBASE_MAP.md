# Codebase Map

Source of truth: current source files. Follow-up verification on 2026-10-10:
20 backend tests pass; Next.js lint, typecheck, and production build pass. The
backend starts against PostgreSQL 18.6, applies Flyway V4 over the existing
V3 database, and passes public discovery/search smoke checks. See `TASKS.md`
for the earlier end-to-end booking verification.

## Frontend (`frontend/`, Next.js 15 App Router)

### Routes

| Route | Source | Behavior |
|---|---|---|
| `/` | `app/(root)/page.tsx` | Public search entry point. |
| `/search` | `app/search/page.tsx` | Public, paginated API-backed barber, service, and location search. |
| `/barbers/[id]` | `app/barbers/[id]/page.tsx` | Public profile, photos, services, reviews, availability, booking request, favourite. |
| `/booking/confirm/[id]` | `app/booking/confirm/[id]/page.tsx` | Loads the signed-in customer's persisted appointment. |
| `/auth/login`, `/auth/signup` | `app/auth/**/page.tsx` | Session login/signup; signup can create a barber account. |
| `/client/dashboard` | `app/client/dashboard/page.tsx` | Customer appointments, favourites, cancellation/reschedule, reviews, profile. |
| `/barber/dashboard` | `app/barber/dashboard/page.tsx` | Requests, today's and upcoming appointments, revenue. |
| `/barber/manage` | `app/barber/manage/page.tsx` | Own profile, editable service names/descriptions/prices/durations, hours, breaks, photos. |
| `/ai/reccomender` | `app/ai/reccomender/page.tsx` | Authenticated recommendation input; shows provider-unavailable response. |
| `/ai/image-generator` | `app/ai/image-generator/page.tsx` | Authenticated local photo preview; does not upload/process without a provider. |
| `/about`, `/contact`, `/for-barbers` | `app/**/page.tsx` | Public informational pages. |

`app/layout.tsx` wraps every route in `AuthProvider` and renders the shared
`Navbar`. `lib/api.ts` is the centralized typed API client, including CSRF
bootstrap, credentials, backend error parsing, and all marketplace calls.
`NEXT_PUBLIC_API_URL` configures the backend URL (default
`http://localhost:8080`). `BARBERING_NEXT_DIST_DIR` optionally selects an
isolated Next.js build-output directory for concurrent local app instances;
the default remains `.next`.

## Backend (`backend/`, Spring Boot 3.5.4, Java 21)

Entry point: `com/Barbering/BarberingAppApplication.java`.

### Controllers

- `AuthController`: signup, login, logout, current user, CSRF.
- `MarketplaceController`: public barber search/profile/services/reviews/
  availability; customer profile; owner-only barber profile/services/hours/
  breaks/appointments.
- `AppointmentController`: customer booking, history, cancel, reschedule,
  review.
- `FavouriteController`: customer favourites.
- `DashboardController`: data-backed customer and barber dashboards.
- `BarberPhotoController`: owner-only photo upload/delete and public opaque-ID
  image streaming.
- `AiController`: authenticated recommendation and try-on request contracts.

Business rules are in `services/MarketplaceService`, `AppointmentService`,
`ReviewService`, `FavouriteService`, and `BarberPhotoService`. `AiProvider` is
the provider seam; `UnavailableAiProvider` returns 503 until a real provider is
configured. `PhotoStorage` is the storage seam; `LocalPhotoStorage` validates
JPEG/PNG/WebP content and stores barber images locally.

### Persistence and security

JPA entities/repositories live in `entities/` and `repositories/`. Flyway
migrations V1-V4 are under `src/main/resources/db/migration/`. Production
Hibernate mode is `validate`. `SecurityConfig` retains HTTP-only cookie
sessions, cookie/header CSRF, role method security, and public read-only
discovery. `CorsConfig` permits one configured frontend origin.

Appointment creation/rescheduling run in a transaction, lock the barber
profile row, then query blocking overlap before insert/update. Appointment
timestamps are stored with offsets; schedule rules use each barber's
`timezone_id`.

### Tests

- `AuthApiIntegrationTests`: session, CSRF, signup/login/logout, and role
  dashboard behavior.
- `BookingConcurrencyIntegrationTests`: two concurrent attempts for one slot
  must produce one appointment and one 409 conflict.
- `BackendApplicationTests`: Spring context startup and public search with
  omitted filters.

Test datasource uses H2/PostgreSQL compatibility mode. Local PostgreSQL is not
available by default unless configured through the `BARBERING_DB_*` variables.
