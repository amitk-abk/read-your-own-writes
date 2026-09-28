#!/usr/bin/env bash
set -euo pipefail

until gosu postgres pg_isready -h primary -U "$REPLICATION_USER" -d postgres; do
  sleep 1
done

install -d -o postgres -g postgres -m 700 "$PGDATA"
rm -rf "$PGDATA"/*
export PGPASSWORD="$REPLICATION_PASSWORD"
gosu postgres pg_basebackup -h primary -D "$PGDATA" -U "$REPLICATION_USER" -Fp -Xs -P -R
unset PGPASSWORD
exec gosu postgres postgres