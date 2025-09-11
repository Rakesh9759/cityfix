-- V4__patch.sql
-- Patch migration to reconcile DB with desired schema without editing V1/V2
-- Safe/idempotent: use IF NOT EXISTS + catalog checks. No explicit BEGIN/COMMIT.

-- ---- Extensions (idempotent) ----
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---- app_user: ensure table/columns/defaults/unique ----
CREATE TABLE IF NOT EXISTS app_user (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email             text NOT NULL UNIQUE,
  password_hash     text NOT NULL,
  role              text NOT NULL CHECK (role IN ('citizen','staff','contractor','admin')),
  is_verified       boolean NOT NULL DEFAULT false,
  email_verified_at timestamptz,
  created_at        timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE app_user
  ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS email text,
  ADD COLUMN IF NOT EXISTS password_hash text,
  ADD COLUMN IF NOT EXISTS role text,
  ADD COLUMN IF NOT EXISTS is_verified boolean,
  ADD COLUMN IF NOT EXISTS email_verified_at timestamptz,
  ADD COLUMN IF NOT EXISTS created_at timestamptz;

-- Ensure UNIQUE on email (robust detection via information_schema)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu
      ON tc.constraint_name = ccu.constraint_name
     AND tc.constraint_schema = ccu.constraint_schema
    WHERE tc.table_name = 'app_user'
      AND tc.constraint_type = 'UNIQUE'
      AND ccu.column_name = 'email'
  ) THEN
    ALTER TABLE app_user ADD CONSTRAINT app_user_email_uk UNIQUE (email);
  END IF;
END$$;

-- ---- team ----
CREATE TABLE IF NOT EXISTS team (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name          text NOT NULL,
  ward_code     text,
  category_mask integer NOT NULL DEFAULT 0,
  on_call_email text,
  created_at    timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE team
  ADD COLUMN IF NOT EXISTS name text,
  ADD COLUMN IF NOT EXISTS ward_code text,
  ADD COLUMN IF NOT EXISTS category_mask integer,
  ADD COLUMN IF NOT EXISTS on_call_email text,
  ADD COLUMN IF NOT EXISTS created_at timestamptz;

ALTER TABLE team
  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN category_mask SET DEFAULT 0;

-- ---- issue (columns, generated geom, checks, timestamps) ----
CREATE TABLE IF NOT EXISTS issue (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title               text NOT NULL,
  description         text,
  category            text NOT NULL,
  severity            text NOT NULL,
  status              text NOT NULL DEFAULT 'NEW',
  reported_by_user_id uuid REFERENCES app_user(id) ON DELETE SET NULL,
  lat                 double precision,
  lon                 double precision,
  geom                geography(Point,4326),
  approx_address      text,
  ward_code           text,
  observed_at         timestamptz,
  sla_due_at          timestamptz,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  location_source     text,
  exif_lat            double precision,
  exif_lon            double precision,
  exif_taken_at       timestamptz
);

ALTER TABLE issue
  ADD COLUMN IF NOT EXISTS title text,
  ADD COLUMN IF NOT EXISTS description text,
  ADD COLUMN IF NOT EXISTS category text,
  ADD COLUMN IF NOT EXISTS severity text,
  ADD COLUMN IF NOT EXISTS status text,
  ADD COLUMN IF NOT EXISTS reported_by_user_id uuid,
  ADD COLUMN IF NOT EXISTS lat double precision,
  ADD COLUMN IF NOT EXISTS lon double precision,
  ADD COLUMN IF NOT EXISTS geom geography(Point,4326),
  ADD COLUMN IF NOT EXISTS approx_address text,
  ADD COLUMN IF NOT EXISTS ward_code text,
  ADD COLUMN IF NOT EXISTS observed_at timestamptz,
  ADD COLUMN IF NOT EXISTS sla_due_at timestamptz,
  ADD COLUMN IF NOT EXISTS created_at timestamptz,
  ADD COLUMN IF NOT EXISTS updated_at timestamptz,
  ADD COLUMN IF NOT EXISTS location_source text,
  ADD COLUMN IF NOT EXISTS exif_lat double precision,
  ADD COLUMN IF NOT EXISTS exif_lon double precision,
  ADD COLUMN IF NOT EXISTS exif_taken_at timestamptz;

ALTER TABLE issue
  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN status SET DEFAULT 'NEW',
  ALTER COLUMN created_at SET DEFAULT now(),
  ALTER COLUMN updated_at SET DEFAULT now();

-- category/severity/status & lat/lon checks
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'issue_category_chk') THEN
    ALTER TABLE issue
      ADD CONSTRAINT issue_category_chk
      CHECK (category IN ('pothole','light','signage','drainage','trash','other'));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'issue_severity_chk') THEN
    ALTER TABLE issue
      ADD CONSTRAINT issue_severity_chk
      CHECK (severity IN ('LOW','MEDIUM','HIGH'));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'issue_status_chk') THEN
    ALTER TABLE issue
      ADD CONSTRAINT issue_status_chk
      CHECK (status IN ('NEW','TRIAGED','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'issue_chk_lat') THEN
    ALTER TABLE issue
      ADD CONSTRAINT issue_chk_lat
      CHECK (lat IS NULL OR (lat BETWEEN -90 AND 90));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'issue_chk_lon') THEN
    ALTER TABLE issue
      ADD CONSTRAINT issue_chk_lon
      CHECK (lon IS NULL OR (lon BETWEEN -180 AND 180));
  END IF;
END$$;

-- Ensure geom is generated; if not possible, install a sync trigger
DO $$
DECLARE
  gen TEXT;
BEGIN
  SELECT is_generated INTO gen
  FROM information_schema.columns
  WHERE table_name = 'issue' AND column_name = 'geom';

  IF gen IS NULL THEN
    EXECUTE 'ALTER TABLE issue ADD COLUMN geom geography(Point,4326)';
    SELECT 'NEVER' INTO gen;
  END IF;

  IF gen = 'ALWAYS' THEN
    -- ok
    NULL;
  ELSIF gen = 'NEVER' THEN
    BEGIN
      EXECUTE 'ALTER TABLE issue DROP COLUMN geom';
      EXECUTE 'ALTER TABLE issue ADD COLUMN geom geography(Point,4326) GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography) STORED';
    EXCEPTION WHEN others THEN
      -- fallback: keep geom in sync via trigger
      CREATE OR REPLACE FUNCTION issue_sync_geom() RETURNS trigger AS $f$
      BEGIN
        NEW.geom := CASE
          WHEN NEW.lat IS NOT NULL AND NEW.lon IS NOT NULL
            THEN ST_SetSRID(ST_MakePoint(NEW.lon, NEW.lat), 4326)::geography
          ELSE NULL
        END;
        RETURN NEW;
      END
      $f$ LANGUAGE plpgsql;

      IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'issue'::regclass AND tgname = 'trg_issue_geom_sync') THEN
        EXECUTE 'CREATE TRIGGER trg_issue_geom_sync BEFORE INSERT OR UPDATE ON issue
                 FOR EACH ROW EXECUTE FUNCTION issue_sync_geom()';
      END IF;

      UPDATE issue
        SET geom = ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography
        WHERE lat IS NOT NULL AND lon IS NOT NULL AND geom IS NULL;
    END;
  END IF;
END$$;

-- updated_at trigger
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'issue'::regclass AND tgname = 'trg_issue_updated') THEN
    EXECUTE 'CREATE TRIGGER trg_issue_updated BEFORE UPDATE ON issue
             FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END$$;

-- ---- child tables ----
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

CREATE TABLE IF NOT EXISTS issue_event (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id      uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  actor_user_id uuid REFERENCES app_user(id) ON DELETE SET NULL,
  type          text NOT NULL,
  payload       jsonb,
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subscription (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id   uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (issue_id, user_id)
);

CREATE TABLE IF NOT EXISTS assignment (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  issue_id         uuid NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
  team_id          uuid NOT NULL REFERENCES team(id) ON DELETE CASCADE,
  assignee_user_id uuid REFERENCES app_user(id) ON DELETE SET NULL,
  assigned_at      timestamptz NOT NULL DEFAULT now()
);

-- ---- indexes (idempotent) ----
CREATE INDEX IF NOT EXISTS idx_issue_geom
  ON issue USING gist (geom);

CREATE INDEX IF NOT EXISTS idx_issue_status_ward_category
  ON issue (status, ward_code, category);

CREATE INDEX IF NOT EXISTS idx_issue_open_status
  ON issue (status)
  WHERE status IN ('NEW','TRIAGED','IN_PROGRESS');

CREATE INDEX IF NOT EXISTS idx_issue_title_trgm
  ON issue USING gin (title gin_trgm_ops);
