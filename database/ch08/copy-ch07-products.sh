#!/usr/bin/env bash
# Optional offline transfer into a freshly migrated Chapter 8 database.
# The source receives only read-only SQL. Applied migration files never change.
set -euo pipefail
: "${CH07_SOURCE_URL:?Set the source PostgreSQL URL without a password}"
: "${CH08_TARGET_URL:?Set the destination PostgreSQL URL without a password}"
: "${CH07_SOURCE_PASSWORD:?Set the source runtime password}"
: "${CH08_TARGET_PASSWORD:?Set the destination migration password}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TRANSFER_DIR="$(mktemp -d)"
chmod 700 "$TRANSFER_DIR"
cleanup() {
  # Remove only the known temporary export, never a caller-selected directory.
  if [[ -f "$TRANSFER_DIR/products.csv" ]]; then
    rm -- "$TRANSFER_DIR/products.csv"
  fi
  rmdir -- "$TRANSFER_DIR"
}
trap cleanup EXIT
cd "$TRANSFER_DIR"
export PGCLIENTENCODING=UTF8
PGPASSWORD="$CH07_SOURCE_PASSWORD" psql -X -q "$CH07_SOURCE_URL" \
  --set ON_ERROR_STOP=1 --file "$SCRIPT_DIR/export-ch07-products.sql"
chmod 600 products.csv
PGPASSWORD="$CH08_TARGET_PASSWORD" psql -X "$CH08_TARGET_URL" \
  --set ON_ERROR_STOP=1 --file "$SCRIPT_DIR/import-ch07-products.sql"
printf 'Transfer committed. Original Chapter 7 data is unchanged. Verify both databases before switching applications.\n'
