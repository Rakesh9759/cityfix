-- speeds up “images by issue” queries
CREATE INDEX IF NOT EXISTS idx_issue_image_issue_created
  ON issue_image (issue_id, created_at DESC);

-- prevent duplicate file-keys per issue (use a unique index; portable and safe in Flyway tx)
CREATE UNIQUE INDEX IF NOT EXISTS uq_issue_image_issue_key
  ON issue_image (issue_id, s3_key);

-- Add status-change columns and metadata payload if missing
ALTER TABLE issue_event
  ADD COLUMN IF NOT EXISTS old_status text,
  ADD COLUMN IF NOT EXISTS new_status text,
  ADD COLUMN IF NOT EXISTS metadata_json text;

-- (Optional) backfill or defaults could go here if you have existing rows
