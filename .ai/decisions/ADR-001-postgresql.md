# ADR-001 --- PostgreSQL as Primary Database

Status: Accepted

## Context

Barbering needs persistent users, barber profiles, services,
availability, appointments, reviews, favourites, and future
reporting/payment-related data.

Appointments and availability require strong relationships, constraints,
transactions, and concurrency protection.

The product also needs image uploads, but image binaries do not require
a document database.

## Decision

Use **PostgreSQL** as the primary application database.

Use: - Spring Data JPA/Hibernate for relational persistence - Flyway for
schema migrations - PostgreSQL JDBC driver

Use object storage for image binaries and PostgreSQL for image
metadata/references.

## Alternatives

MongoDB was considered.

It remains possible for specialized future services, but it is not the
primary application database.

## Consequences

Positive: - strong relational modeling - transactions - constraints -
reliable appointment integrity - flexible querying/search -
reporting/earnings support - JSONB available when semi-structured data
is useful

Tradeoff: - relational schema design requires deliberate modeling and
migrations.

## Scope

This decision does not select a specific cloud object-storage provider.
