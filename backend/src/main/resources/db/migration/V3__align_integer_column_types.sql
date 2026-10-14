-- Match numeric columns that are represented as Java int properties.
ALTER TABLE working_hours ALTER COLUMN day_of_week TYPE INTEGER;
ALTER TABLE barber_breaks ALTER COLUMN day_of_week TYPE INTEGER;
ALTER TABLE reviews ALTER COLUMN rating TYPE INTEGER;
