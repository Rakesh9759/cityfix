#!/usr/bin/env bash
set -euo pipefail

ENDPOINT="http://localhost:4566"

# Use test creds for Localstack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION="${AWS_REGION:-us-east-1}"

BUCKET="${S3_BUCKET:-cityfix-dev}"
FROM="${SES_FROM:-noreply@dev.yourcity.dev}"

echo "=> Creating S3 bucket: $BUCKET"
aws --endpoint-url "$ENDPOINT" s3api create-bucket \
  --bucket "$BUCKET" \
  --create-bucket-configuration LocationConstraint=$AWS_REGION >/dev/null 2>&1 || true

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
