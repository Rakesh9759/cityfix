-- GiST index on geography for spatial queries
CREATE INDEX IF NOT EXISTS idx_issue_geom
  ON issue USING gist (geom);

-- Common filter combo
CREATE INDEX IF NOT EXISTS idx_issue_status_ward_category
  ON issue (status, ward_code, category);

-- Partial index for "open" states
CREATE INDEX IF NOT EXISTS idx_issue_open_status
  ON issue (status)
  WHERE status IN ('NEW','TRIAGED','IN_PROGRESS');

-- Trigram title search (requires pg_trgm)
CREATE INDEX IF NOT EXISTS idx_issue_title_trgm
  ON issue USING gin (title gin_trgm_ops);