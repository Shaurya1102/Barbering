# API Agent Instructions

Frontend/backend communicate through HTTP APIs.

The API now includes auth/profile, public barber discovery, barber management,
availability, customer booking and appointment lifecycle, reviews,
favourites, photo metadata/content, dashboards, and unavailable AI contracts.

When changing APIs: 1. find all callers 2. inspect request/response
models 3. assess compatibility 4. update consumers 5. test end-to-end

As APIs mature, use structured JSON, appropriate status codes,
validation, consistent errors, and no sensitive-data leakage.

Do not silently break existing consumers.
