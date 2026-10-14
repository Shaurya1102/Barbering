# Frontend Agent Instructions

Stack: Next.js 15.3.5, React 19, TypeScript strict, App Router, Tailwind
CSS v4.

Keep server components server-side unless client behavior is required.
Reuse components/types. Do not hard-code new environment-specific URLs.

Authentication state comes from the backend's HTTP-only cookie session.
Do not introduce localStorage token auth.

Discovery should be public. Booking, dashboards, favourites, appointment
management, and AI require authentication.

AI UX should be simple: input → selection → generation/recommendation →
result.

Inspect route groups/layouts before routing changes. Do not silently
rename public routes.

Use actual frontend scripts for verification and report only executed
checks.
