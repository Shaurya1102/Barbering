# Barbering --- Product Definition

## Goal

Build a polished barber discovery and appointment platform with
AI-assisted hairstyle exploration.

## Public Experience

Users do not need accounts to: - search barbers - search by location -
search by service - view barber profiles - see services/prices - see
ratings - see appropriate availability - eventually see wait-time
information

## Customer Experience

Authenticated customers can: - manage account - book appointments -
cancel/reschedule - view history - favourite barbers - use AI features

## Barber Experience

Authenticated barbers can: - create/manage profile - upload photos -
manage services/prices/duration - set working hours/breaks - manage
availability - accept/reject appointments - calendar - earnings -
customer management

## Auth

Email/password and optional Google OAuth.

Central auth is Spring Security with secure HTTP-only cookie sessions.
Both login methods resolve to one application User.

## Booking

availability → slots → service/slot → request → accept/reject →
confirmation → reminder → appointment → completed/cancelled/no-show.

Payments are deferred.

## AI

### Hairstyle Try-On

Photo → style selection → generation → preview → optional relevant
service/barber.

### Style Recommender

Hair/face characteristics + preferences + maintenance needs +
barber/service data → explained style recommendations → optional
service/barber → booking.

AI is authentication-gated and should be easy for non-technical users.

## Data

PostgreSQL is the approved database. Image binaries should use object
storage; Postgres stores metadata/references.
