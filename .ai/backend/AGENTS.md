# Backend Agent Instructions

Stack: Spring Boot 3.5.4, Java 21, Maven, Lombok.

Architecture: Controller → Service → Repository → PostgreSQL via
JPA/Hibernate. Use Flyway for schema migrations.

Current repo includes Spring Security sessions, PostgreSQL/Flyway, JPA
entities/repositories, and controller/service/repository flows for profiles,
services, discovery, photos, availability, appointments, reviews, favourites,
and dashboards. AI endpoints use an unavailable-provider boundary until real
infrastructure is configured.

Do not invent existing layers. Introduce them when required by the
feature.

Security is centralized in Spring Security. Authentication uses
HTTP-only cookie sessions. Google OAuth uses Spring Security OAuth2
Client.

Controllers handle HTTP concerns; services business logic; repositories
persistence.

Never store/log plaintext credentials or secrets. Backend must enforce
authorization.

Use Maven wrapper for verification and never claim tests passed unless
executed.
