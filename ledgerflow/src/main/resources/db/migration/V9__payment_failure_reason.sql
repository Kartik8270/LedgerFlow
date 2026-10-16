-- Phase 2/3: Add failure_reason column to payments.
ALTER TABLE payments ADD COLUMN failure_reason VARCHAR(512);
