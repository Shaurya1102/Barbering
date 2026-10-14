# Database Agent Instructions

PostgreSQL is accepted as the primary database. Do not reconsider
MongoDB.

Use Spring Data JPA/Hibernate and Flyway unless a new ADR changes this.

Store structured application data in PostgreSQL. Store image binaries in
object storage; keep metadata/object keys in PostgreSQL.

Schema design must support users, auth identities, barbers, services,
availability, appointments, reviews, favourites, and future
notifications.

Service duration is required for real availability calculation.

Appointment creation must prevent concurrent double booking.

Do not add future tables without a feature requiring them. Do not commit
credentials.
