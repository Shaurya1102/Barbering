# Barbering

Barber discovery and booking application. The frontend is Next.js 15.3.5,
React 19, TypeScript, and Tailwind CSS 4. The backend is Spring Boot 3.5.4,
Java 21, Spring Security, Spring Data JPA, PostgreSQL, and Flyway.

## Current features

- Email/password signup and login with BCrypt, HTTP-only session cookies, and
  CSRF protection.
- Public barber search and profiles with services, prices, ratings, and
  availability.
- Barber profile, service, weekly hours, recurring break, and photo
  management.
- Customer appointment requests, cancellation/rescheduling, reviews, and
  favourites; barber accept/reject/complete/no-show actions.
- Customer and barber dashboards.
- AI routes and UI are present, but no AI provider is configured. The
  hairstyle page keeps the selected image in the browser and does not upload
  it. Barber portfolio photos use configurable local storage for development.

## Requirements

- Node.js and npm
- Java 21
- PostgreSQL

## Run locally

Create a PostgreSQL database and set the backend environment variables before
starting the API:

- `BARBERING_DB_URL` (default `jdbc:postgresql://localhost:5432/barbering`)
- `BARBERING_DB_USER` (default `barbering`)
- `BARBERING_DB_PASSWORD`
- `BARBERING_FRONTEND_ORIGIN` (default `http://localhost:3000`)
- `BARBERING_UPLOAD_DIR` (default `./uploads/barber-photos`)

From PowerShell, start the backend:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

In another terminal, start the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The redesigned UI is the
application source used by the standard `dev`, `build`, and `start` scripts;
all three use Next.js's default `.next` output directory. Do not set
`BARBERING_NEXT_DIST_DIR` for ordinary development or production runs. It is
optional for an isolated preview that must run alongside another frontend.

The frontend API client defaults to `http://localhost:8080`, and backend CORS
defaults to `http://localhost:3000`, so the normal local workflow needs no
origin overrides. When developing from a different public frontend origin,
set `BARBERING_FRONTEND_ORIGIN` to that exact origin.

## Checks

```powershell
# From frontend/
npm run lint
npm run typecheck
npm run build

# From backend/
./mvnw.cmd test
```

The automated backend tests use H2 in PostgreSQL compatibility mode and the
same migrations. PostgreSQL remains the production database. See `.ai/API.md`,
`.ai/DATABASE.md`, and `.ai/CODEBASE_MAP.md` for API, schema, and source
details.
