-- A service offering belongs to one barber. Keep edits local to that offering;
-- the shared catalog remains a source of defaults for existing records.
ALTER TABLE barber_services
    ADD COLUMN name_override VARCHAR(100);

ALTER TABLE barber_services
    ADD COLUMN description_override VARCHAR(1000);
