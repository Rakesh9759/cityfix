#!/usr/bin/env bash
set -euo pipefail

# Run from repo root no matter where called from
cd "$(dirname "$0")/.."

# Load .env.dev into the current shell (host side)
if [ -f .env.dev ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env.dev
  set +a
fi

ENDPOINT="http://localhost:4566"

# Use test creds for Localstack (not your real AWS)
export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID:-test}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY:-test}
export AWS_REGION=${AWS_REGION:-us-east-1}

BUCKET="${S3_BUCKET:-cityfix-dev}"
FROM="${SES_FROM:-noreply@dev.yourcity.dev}"

retry() {
  local tries=$1; shift
  local delay=$1; shift
  local i=1
  until "$@"; do
    if [ $i -ge $tries ]; then return 1; fi
    sleep "$delay"
    i=$((i+1))
  done
}

echo "=> Ensuring S3 is ready..."
retry 30 2 aws --endpoint-url "$ENDPOINT" s3 ls >/dev/null

echo "=> Creating S3 bucket: $BUCKET (region=$AWS_REGION)"
if [ "$AWS_REGION" = "us-east-1" ]; then
  aws --endpoint-url "$ENDPOINT" s3api create-bucket \
    --bucket "$BUCKET" >/dev/null 2>&1 || true
else
  aws --endpoint-url "$ENDPOINT" s3api create-bucket \
    --bucket "$BUCKET" \
    --create-bucket-configuration LocationConstraint="$AWS_REGION" >/dev/null 2>&1 || true
fi

echo "=> Waiting for bucket existence: $BUCKET"
retry 30 2 aws --endpoint-url "$ENDPOINT" s3api head-bucket --bucket "$BUCKET"

echo "=> Setting S3 CORS on $BUCKET"
aws --endpoint-url "$ENDPOINT" s3api put-bucket-cors --bucket "$BUCKET" --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["PUT","GET","HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }]
}' >/dev/null

echo "=> Verifying SES identity: $FROM"
aws --endpoint-url "$ENDPOINT" ses verify-email-identity --email-address "$FROM" >/dev/null 2>&1 || true

echo "=> Writing sample SSM params"
aws --endpoint-url "$ENDPOINT" ssm put-parameter --name "/cityfix/dev/S3_BUCKET" --type String --value "$BUCKET" --overwrite >/dev/null

echo "=> Done. S3 and SES are ready in Localstack."

