# Authentication Agent Instructions

Accepted architecture: - Spring Security in backend - HTTP-only
cookie-based sessions - PostgreSQL-backed session persistence when
needed - email/password - optional Google OAuth through Spring Security
OAuth2 Client

Both auth methods resolve to one application User identity.

Do not use localStorage/sessionStorage for auth tokens. Do not create a
separate Google-only user model.

Minimum roles: CUSTOMER and BARBER; keep extensible for ADMIN.

Public discovery remains accessible anonymously. Backend must enforce
protected operations.

AI, booking, appointment management, favourites, dashboards, and
barber-management endpoints require appropriate
authentication/authorization.

Use secure password hashing. Cookie-based auth requires deliberate
CSRF/CORS handling; do not disable security simply to make local
development work.

Do not ask the user to choose an auth architecture again; these
decisions are accepted.
