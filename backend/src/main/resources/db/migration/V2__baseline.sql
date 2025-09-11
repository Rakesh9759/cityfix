-- Users & Teams first (FK targets)
CREATE TABLE IF NOT EXISTS app_user (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email             text NOT NULL UNIQUE,
  password_hash     text NOT NULL,
  role              text NOT NULL CHECK (role IN ('citizen','staff','contractor','admin')),
  is_verified       boolean NOT NULL DEFAULT false,
  email_verified_at timestamptz,
  created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS team (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name          text NOT NULL,
  ward_code     text,
  category_mask integer NOT NULL DEFAULT 0,
  on_call_email text,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- Issues (geom generated from lon/lat; validates ranges)
CREATE TABLE IF NOT EXISTS issue (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title               text NOT NULL,
  description         text,
  category            text NOT NULL CHECK (category IN ('pothole','light','signage','drainage','trash','other')),
  severity            text NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH')),
  status              text NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','TRIAGED','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED')),
  reported_by_user_id uuid REFERENCES app_user(id) ON DELETE SET NULL,

  lat                 double precision,
  lon                 double precision,
  geom                geography(Point,4326)
                       GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography) STORED,

  approx_address      text,
  ward_code           text,

  observed_at         timestamptz,
  sla_due_at          timestamptz,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),

  location_source     text CHECK (location_source IN ('user','exif','admin')),
  exif_lat            double precision,
  exif_lon            double precision,
  exif_taken_at       timestamptz,

  CONSTRAINT chk_lat CHECK (lat IS NULL OR (lat BETWEEN -90 AND 90)),
  CONSTRAINT chk_lon CHECK (lon IS NULL OR (lon BETWEEN -180 AND 180))
);

-- Images per issue
CREATE TABLE IF NOT EXISTS issue_image (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id     uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  s3_key       text NOT NULL,
  content_type text NOT NULL,
  width        integer,
  height       integer,
  is_thumb     boolean NOT NULL DEFAULT false,
  created_at   timestamptz NOT NULL DEFAULT now()
);

-- Timeline / audit log
CREATE TABLE IF NOT EXISTS issue_event (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id      uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  actor_user_id uuid REFERENCES app_user(id) ON DELETE SET NULL,
  type          text NOT NULL,
  payload       jsonb,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- Subscriptions (email updates)
CREATE TABLE IF NOT EXISTS subscription (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id   uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (issue_id, user_id)
);

-- Assignments
CREATE TABLE IF NOT EXISTS assignment (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id         uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  team_id          uuid NOT NULL REFERENCES team(id) ON DELETE CASCADE,
  assignee_user_id uuid REFERENCES app_user(id) ON DELETE SET NULL,
  assigned_at      timestamptz NOT NULL DEFAULT now()
);

-- updated_at trigger for issue
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_issue_updated ON issue;
CREATE TRIGGER trg_issue_updated BEFORE UPDATE ON issue
FOR EACH ROW EXECUTE FUNCTION set_updated_at();