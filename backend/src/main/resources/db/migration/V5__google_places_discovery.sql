-- Google Places discovery support: member profiles may carry coordinates and an
-- optional Google place id (populated when a claim is approved). Claims record
-- the ownership-request lifecycle for a public Google Maps listing. Only the
-- place id and claim metadata are stored; full place payloads stay with Google.
ALTER TABLE barber_profiles
    ADD COLUMN latitude DOUBLE PRECISION;

ALTER TABLE barber_profiles
    ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE barber_profiles
    ADD COLUMN google_place_id VARCHAR(64);

CREATE UNIQUE INDEX uq_barber_profiles_google_place_id ON barber_profiles (google_place_id);

CREATE TABLE place_claims (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    barber_profile_id BIGINT NOT NULL REFERENCES barber_profiles(id) ON DELETE CASCADE,
    google_place_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_place_claims_google_place_id UNIQUE (google_place_id),
    CONSTRAINT ck_place_claims_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_place_claims_profile ON place_claims (barber_profile_id);
