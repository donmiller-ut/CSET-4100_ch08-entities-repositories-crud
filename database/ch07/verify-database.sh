#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

if [[ -z "${NORTHSTAR_APP_URL:-}" ]]; then
    printf 'Set NORTHSTAR_APP_URL to the untracked PostgreSQL runtime connection URL.\n' >&2
    exit 2
fi

psql "$NORTHSTAR_APP_URL" \
    --set ON_ERROR_STOP=1 \
    --file database/ch07/verify-allowed-operations.sql

expect_rejection() {
    local label="$1"
    local statement="$2"

    if psql "$NORTHSTAR_APP_URL" \
            --set ON_ERROR_STOP=1 \
            --command "BEGIN; $statement; ROLLBACK;" >/dev/null 2>&1; then
        printf 'FAILED: PostgreSQL accepted %s.\n' "$label" >&2
        exit 1
    fi
    printf 'Verified rejection: %s\n' "$label"
}

expect_rejection 'schema creation by the runtime role' \
    'CREATE TABLE northstar.runtime_role_must_not_create_objects (id integer)'
expect_rejection 'a Product without a required name' \
    'INSERT INTO northstar.product (price) VALUES (1.00)'
expect_rejection 'a Product price below 0.01' \
    "INSERT INTO northstar.product (name, price) VALUES ('Invalid price', 0.00)"
expect_rejection 'a Product price with more than two fractional digits' \
    "INSERT INTO northstar.product (name, price) VALUES ('Invalid scale', 1.001)"
expect_rejection 'a blank Product name' \
    "INSERT INTO northstar.product (name, price) VALUES ('   ', 1.00)"
expect_rejection 'a Product name longer than 120 characters' \
    "INSERT INTO northstar.product (name, price) VALUES (repeat('n', 121), 1.00)"

printf 'Chapter 7 PostgreSQL privilege and constraint verification passed.\n'
