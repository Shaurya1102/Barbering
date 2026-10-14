# ADR-002 --- Spring Security Central Authentication

Status: Accepted

## Context

Barbering has a Spring Boot backend and a Next.js frontend.
Authentication must support email/password and optional Google OAuth,
while backend authorization must protect booking, AI, dashboards, and
barber operations.

## Decision

Centralize authentication and authorization in Spring Security on the
Spring Boot backend.

Use secure HTTP-only cookie-based sessions.

Use Spring Security OAuth2 Client for Google OAuth.

Persist sessions with Spring Session JDBC when persistent session
storage is required.

Both email/password and Google OAuth resolve to one application User
identity.

## Alternatives

Frontend-managed authentication such as NextAuth/Auth.js was considered
but is not the selected architecture.

JWTs stored in browser storage are not selected.

## Consequences

Positive: - one central security boundary - backend can enforce
authorization directly - HTTP-only cookies reduce token exposure to
JavaScript - OAuth and password authentication converge on one user
model

Tradeoff: - CSRF/CORS/cookie behavior must be configured carefully -
frontend/backend development remains cross-origin locally unless proxied

## Security

Never store passwords in plaintext or tokens in
localStorage/sessionStorage.
