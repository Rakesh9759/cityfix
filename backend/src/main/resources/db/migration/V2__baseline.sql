-- USERS
CREATE TABLE IF NOT EXISTS users (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email       TEXT NOT NULL UNIQUE,
  name        TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ISSUES
CREATE TABLE IF NOT EXISTS issues (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title       TEXT NOT NULL,
  description TEXT,
  status      TEXT NOT NULL DEFAULT 'new' CHECK (status IN ('new','triaged','in_progress','resolved','closed')),
  reporter_id UUID REFERENCES users(id) ON DELETE SET NULL,
  -- store location as geometry in WGS84 (lon/lat)
  location    geometry(Point, 4326),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- IMAGES (for issue photos)
CREATE TABLE IF NOT EXISTS images (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id     UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
  s3_key       TEXT NOT NULL,
  content_type TEXT,
  width        INT,
  height       INT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(issue_id, s3_key)
);

-- EVENTS (state changes, comments, system notices)
CREATE TABLE IF NOT EXISTS events (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id   UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
  type       TEXT NOT NULL,
  payload    JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_issues_status ON issues(status);
CREATE INDEX IF NOT EXISTS idx_issues_location ON issues USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_events_issue_id ON events(issue_id);
CREATE INDEX IF NOT EXISTS idx_events_payload_gin ON events USING GIN (payload);

-- updated_at trigger
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_issues_set_updated_at ON issues;
CREATE TRIGGER trg_issues_set_updated_at
BEFORE UPDATE ON issues
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
