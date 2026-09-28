#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator';
CREATE ROLE ryow WITH LOGIN PASSWORD 'ryow';
GRANT CONNECT ON DATABASE ryow TO ryow;
SQL

cat >> "$PGDATA/pg_hba.conf" <<'HBA'
host replication replicator 0.0.0.0/0 scram-sha-256
host all all 0.0.0.0/0 scram-sha-256
HBA