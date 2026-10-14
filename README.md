# 💈 Barbering

**Barber discovery and booking application.** Find barbers, book appointments, manage services — with AI-assisted styling built in.

| | |
|---|---|
| **Frontend** | Next.js 15.3.5 · React 19 · TypeScript · Tailwind CSS 4 |
| **Backend** | Spring Boot 3.5.4 · Java 21 · Spring Security · Spring Data JPA · PostgreSQL · Flyway |

## 📑 Contents

- [Current features](#current-features)
- [Requirements](#requirements)
- [Run locally](#run-locally)
- [Checks](#checks)
- [Documentation](#documentation)

---

## ✨ Current features

**Authentication & accounts**

- Email/password signup and login with BCrypt, HTTP-only session cookies, and CSRF protection.

**Public discovery (no account needed)**

- Public barber search and profiles with services, prices, ratings, and availability.

**Barber management**

- Barber profile, service, weekly hours, recurring break, and photo management.
- Accept / reject / complete / no-show actions on appointments.

**Customer booking**

- Customer appointment requests, cancellation/rescheduling, reviews, and favourites.

**Dashboards**

- Customer and barber dashboards.

**AI**

- AI routes and UI are present, but no AI provider is configured. The hairstyle page keeps the selected image in the browser and does not upload it. Barber portfolio photos use configurable local storage for development.

---

## 🧰 Requirements

| Tool | Needed for |
|---|---|
| Node.js + npm | Frontend (Next.js) |
| Java 21 | Backend (Spring Boot) |
| PostgreSQL | Production database |

---

## 🚀 Run locally

**1. Create a PostgreSQL database**, then set the backend environment variables before starting the API:

| Variable | Default |
|---|---|
| `BARBERING_DB_URL` | `jdbc:postgresql://localhost:5432/barbering` |
| `BARBERING_DB_USER` | `barbering` |
| `BARBERING_DB_PASSWORD` | *(no default — set it)* |
| `BARBERING_FRONTEND_ORIGIN` | `http://localhost:3000` |
| `BARBERING_UPLOAD_DIR` | `./uploads/barber-photos` |

**2. Start the backend** (PowerShell):

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

**3. Start the frontend** (another terminal, PowerShell):

```powershell
cd frontend
npm install
npm run dev
```

**4. Open [http://localhost:3000](http://localhost:3000).**

> ⚠️ **About `BARBERING_NEXT_DIST_DIR`** — The redesigned UI is the application source used by the standard `dev`, `build`, and `start` scripts; all three use Next.js's default `.next` output directory. Do not set `BARBERING_NEXT_DIST_DIR` for ordinary development or production runs. It is optional for an isolated preview that must run alongside another frontend.

> ℹ️ **Origins** — The frontend API client defaults to `http://localhost:8080`, and backend CORS defaults to `http://localhost:3000`, so the normal local workflow needs no origin overrides. When developing from a different public frontend origin, set `BARBERING_FRONTEND_ORIGIN` to that exact origin.

---

## ✅ Checks

```powershell
# From frontend/
npm run lint
npm run typecheck
npm run build

# From backend/
./mvnw.cmd test
```

The automated backend tests use H2 in PostgreSQL compatibility mode and the same migrations. PostgreSQL remains the production database.

---

## 📚 Documentation

| Doc | What it covers |
|---|---|
| [`.ai/API.md`](.ai/API.md) | API contract details |
| [`.ai/DATABASE.md`](.ai/DATABASE.md) | Schema details |
| [`.ai/CODEBASE_MAP.md`](.ai/CODEBASE_MAP.md) | Source layout and navigation |
