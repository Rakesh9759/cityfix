DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_category_check')
     AND EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_category_chk') THEN
    ALTER TABLE issue DROP CONSTRAINT issue_category_check;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_severity_check')
     AND EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_severity_chk') THEN
    ALTER TABLE issue DROP CONSTRAINT issue_severity_check;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_status_check')
     AND EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_status_chk') THEN
    ALTER TABLE issue DROP CONSTRAINT issue_status_check;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_lat')
     AND EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_chk_lat') THEN
    ALTER TABLE issue DROP CONSTRAINT chk_lat;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_lon')
     AND EXISTS (SELECT 1 FROM pg_constraint WHERE conname='issue_chk_lon') THEN
    ALTER TABLE issue DROP CONSTRAINT chk_lon;
  END IF;
END$$;
