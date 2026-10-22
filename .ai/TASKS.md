# Barbering Roadmap

## Foundation and authentication

- [x] PostgreSQL/Flyway, Spring Data JPA, and API error conventions
- [x] BCrypt email/password signup and login
- [x] HTTP-only cookie session, logout, `/auth/me`, and double-submit CSRF
- [x] CUSTOMER/BARBER backend roles and authenticated frontend state
- [x] Barber account signup creates its profile
- [ ] Google OAuth and account linking
- [ ] Password recovery and email verification

## Barber profiles, services, and availability

- [x] Customer display-name profile read/update
- [x] Barber public profile and owner-only profile updates
- [x] Barber-owned service create/read/edit/disable, including names,
  descriptions, prices, durations, and enabled state
- [x] Database-backed barber search by name, bio, edited service names, and
  street/city/region/postal-code/country location
- [x] Barber photos with local storage abstraction and safe opaque URLs
- [x] Weekly working hours and recurring breaks
- [x] Availability derived from time zone, hours, breaks, service duration,
  existing appointments, and past-time rules

## Booking, reviews, and customer features

- [x] Pending booking request and barber accept/reject
- [x] Customer history, cancellation, and rescheduling
- [x] Barber complete/no-show transitions
- [x] Transactional barber-row lock to serialize conflicting bookings
- [x] One review per completed appointment and public rating summaries
- [x] Customer favourites with a unique database constraint
- [x] Customer dashboard with appointment groups, review actions, favourites,
  and profile access
- [ ] Reminder delivery

## Barber workspace and navigation

- [x] Barber dashboard with requests, today's/upcoming schedule, and completed
  revenue
- [x] Barber profile/services/hours/breaks/photo management UI
- [x] Shared responsive navigation and public search/profile/booking paths
- [ ] Dedicated calendar view and customer-management tools

## AI

- [x] Authenticated recommendation and try-on API contracts
- [x] Provider interface and explicit unavailable state; no fake result is
  returned while no provider is configured
- [x] Recommendation input form and local-only image preview
- [ ] Connected style recommendation model/provider
- [ ] Uploaded-image try-on provider
- [ ] Match recommendations to services/barbers

## Google Places discovery & place claims

- [x] Public hybrid nearby discovery endpoint (Google Maps + database, with
  distance/rating/open-now filtering and claimed-place exclusion)
- [x] Barber Google place claim request/approve/reject lifecycle with current-claim read
- [x] V5 migration storing coordinates + place id on the profile at approved claim

## Future scope

- [ ] Payments
- [ ] Walk-in queue/wait time
- [ ] Shop/multi-barber model
- [ ] Notifications expansion
- [ ] Admin
- [ ] Deployment and CI/CD

## Verification status

Earlier verification on 2026-10-10:

- All 17 backend tests pass against H2, including auth/CSRF, role checks,
  omitted-filter public search, and concurrent booking conflict coverage.
- The local PostgreSQL 18.6 `barbering` database migrated to V3 and passed
  Hibernate schema validation at backend startup.
- An isolated PostgreSQL end-to-end flow passed barber signup/profile/service/
  hours setup, filtered public search, availability, customer booking, slot
  blocking, barber confirmation, and customer history. The temporary database
  was removed afterward.
- Frontend `npm run lint`, `npm run typecheck`, and `npm run build` pass.
- Live smoke checks returned 200 for frontend `/` and `/search`, backend
  filtered discovery, and credentialed PATCH CORS; anonymous `/auth/me`
  returned 401.

Backend tests were run with the repository's cached Maven and JDK 21 using
`mvn -o -Dmaven.repo.local=../tools/m2repo -DforkCount=0 test`. The test JVM
used the cached Mockito core JAR as a startup agent because dynamic self-attach
is blocked in this Windows runner. Frontend checks used the `lint`,
`typecheck`, and `build` package scripts.

Follow-up implementation verification on 2026-10-10:

- Backend Maven tests: 20 tests pass against H2. Coverage includes the V4
  migration, customer profile read/edit, barber service create/edit/enable/
  disable, owner enforcement, edited-service search, and location search.
- Frontend `npm run lint`, `npm run typecheck`, and `npm run build` pass.
- A separate backend instance connected to the existing PostgreSQL 18.6
  `barbering` database, applied Flyway V4 over V3, and passed Hibernate schema
  validation. No rows or database objects were dropped or reset.
- Read-only runtime smoke checks returned 200 for backend CSRF/public search,
  including query, service, and location filters, plus the frontend home,
  search, and barber management routes. Mutating runtime checks were not run
  against the existing database to avoid leaving test accounts or bookings.

Final end-to-end verification on 2026-10-13:

- Backend Maven suite: BUILD SUCCESS with Tests run: 23, Failures: 0,
  Errors: 0, Skipped: 0 (AuthApiIntegrationTests 14, BackendApplicationTests
  2, BookingConcurrencyIntegrationTests 1, MarketplaceApiIntegrationTests 6)
  against H2 in-memory with Flyway V1-V4.
- Fixed `ApiExceptionHandler` for Spring Boot 3.5.4 / spring-web 6.2.9:
  `HandlerMethodValidationException` no longer exposes
  `getConstraintViolations()`, so the handler now maps
  `getParameterValidationResults()` to resolvable error default messages and
  returns 400 "Validation failed." with sorted details.
- Frontend `npm run lint`, `npm run typecheck`, and `npm run build` pass
  (Next.js 15.3.5; all 16 routes generated). Browser journeys were not
  automated in this session.
- Runtime verification used a disposable isolated PostgreSQL cluster on port
  5433 because the existing local PostgreSQL 18 on port 5432 requires a
  password and no credentials exist in the repository. Flyway applied V1-V4
  to PostgreSQL 18.6, Hibernate schema validation passed with
  ddl-auto=validate, and Tomcat served on port 8080.
- Full HTTP flow against that database was green: barber login (200), profile
  update (200), service create (201), weekly working hours with ISO day names
  (200), public search by query, services list, availability on a 15-minute
  grid in Asia/Kolkata, customer signup (201), booking POST /appointments
  (201 PENDING with startAt normalized to UTC), barber status PATCH to
  CONFIRMED (200), customer appointment history showing the confirmed
  booking, bad login with CSRF token (401), and POST /ai/recommendations with
  a valid payload returning the designed 503 unavailable response.
- Cleanup: the disposable cluster was stopped and its data directory
  (`tools/pg-e2e`) deleted. `DROP DATABASE` itself hung because orphaned psql
  client backends from timed-out attempts survived; teardown plus data-dir
  deletion removed all disposable data. The existing PostgreSQL on port 5432
  was never connected to and remains untouched.

Follow-up verification — Google Places discovery & place claims recovery on 2026-10-20:

- Backend Maven suite: BUILD SUCCESS with Tests run: 55, Failures: 0, Errors:
  0, Skipped: 0 (AuthApiIntegrationTests 14, BackendApplicationTests 2,
  BookingConcurrencyIntegrationTests 1, MarketplaceApiIntegrationTests 6,
  DiscoveryServiceTest 17, PlaceClaimServiceTest 15) against H2 in-memory with
  Flyway V1-V5.
- Fixed DTO property mismatch: `PlaceClaim.requestAt` aligned to `requestedAt`
  matching backend DTO.
- Fixed null-safety: default empty list for optional `services` param in
  `DiscoveryController` and `DiscoveryService` preventing NPE.
- Aligned frontend types: added `googleAttribution`, `directionsUri`, and
  `reviewFreshness` to `DiscoveryResult` / `DiscoveryListing`.
- Added optional `latitude` and `longitude` coordinates to `BarberProfileUpdate`
  allowing member barbers to be discovered nearby even without Google claims.
- Frontend checks: `npm run lint` (clean, 0 warnings), `npm run typecheck`
  (clean, 0 errors), and `npm run build` (Next.js 15.3.5; all 16 routes
  generated).
- Live runtime verification: ran against an isolated disposable PostgreSQL 18.6
  cluster on port 5433 (leaving existing 5432 database untouched). Flyway V1-V5
  applied cleanly, Hibernate schema validation succeeded with
  `ddl-auto=validate`, and Tomcat served on port 8080.
- Verified full HTTP journeys:
  - Barber signup (`POST /auth/signup`), login, and session cookie handling.
  - Coordinate update (`PUT /barber/me/profile`).
  - Service creation (`POST /barber/me/services`).
  - Weekly working hours (`PUT /barber/me/working-hours`).
  - Slot availability (`GET /barbers/1/availability`) returning 35 slots.
  - Customer signup, booking creation (`POST /appointments` -> PENDING).
  - Barber booking confirmation (`PATCH /barber/me/appointments/{id}/status` -> CONFIRMED).
  - Customer appointment history (`GET /appointments/mine`).
  - Public hybrid nearby discovery (`GET /discovery/nearby`) returning the member
    barber at 0.33 km with directions link and honest `googleAvailable=false`
    status without an API key.
  - Google place claim submission (`POST /barber/me/google-claim` -> PENDING) and
    status retrieval (`GET /barber/me/google-claim`).
- Environment template: added root `.env.example` documenting all backend,
  frontend, database, and Google Places configuration. All disposable test
  data was cleaned up.
