# Read Your Own Writes with PostgreSQL Replicas

This project demonstrates a timestamp-based read-your-own-write (RYOW) policy in Spring Boot. Writes always use a PostgreSQL primary. Reads normally alternate between two streaming replicas, but a read carrying a recent write token is sent to the primary for one minute.

## Architecture

```text
POST/PUT --readWrite--> PostgreSQL primary :5432
GET (no recent token) --readOnlyFirst/readOnlySecond--> PostgreSQL replicas :5433/:5434
GET (token younger than 60 seconds) --readWrite--> PostgreSQL primary :5432
```

The application has three explicitly named `JdbcTemplate` paths: `readWriteJdbcTemplate`, `readOnlyFirstJdbcTemplate`, and `readOnlySecondJdbcTemplate`. Replica selection is round-robin to keep demonstrations repeatable. The response header `X-RYOW-Source` shows which connection served a read.

## Requirements

- Java 21
- Maven, or the included `./mvnw`
- Podman and Podman Compose support (`podman compose`)

## PostgreSQL Cluster

### What is being created?

This example runs three separate PostgreSQL 16 containers on one local machine:

| Container | Role | Host port | Container port |
| --- | --- | ---: | ---: |
| `primary` | Read-write primary | `5432` | `5432` |
| `replica-first` | Read-only streaming replica | `5433` | `5432` |
| `replica-second` | Read-only streaming replica | `5434` | `5432` |

The two replicas are not independent databases with separate application data. They continuously copy PostgreSQL write-ahead log (WAL) records from the primary. A transaction is written to the primary first, recorded in WAL, and then replayed by each replica. PostgreSQL calls this physical streaming replication.

This is a local demonstration cluster, not a production high-availability installation. All three containers run on the same Podman machine, and the replication connection uses simple local demonstration credentials.

### Directory and file structure

The `podman/` directory contains the complete database setup:

```text
podman/
├── compose.yml                 # Defines the three containers, ports, volumes, and startup order
├── replica-entrypoint.sh       # Initializes each replica from the primary
└── init/
  ├── 00-primary-init.sh      # Runs once on a new primary volume
  └── 01-schema.sql            # Creates the application table and grants permissions
```

`compose.yml` is the central file. It tells Podman which PostgreSQL image to use, which environment variables to provide, which host ports to publish, and which persistent volumes to mount. The primary mounts the `podman/init/` directory into PostgreSQL's `/docker-entrypoint-initdb.d/` directory. The official PostgreSQL image runs files in that directory only when it initializes an empty data directory.

The three named volumes keep database files separate:

```text
primary-data        -> primary PostgreSQL data directory
replica-first-data  -> first replica PostgreSQL data directory
replica-second-data -> second replica PostgreSQL data directory
```

The replicas do not mount the primary's volume. Each replica creates its own local copy using `pg_basebackup`, then receives subsequent WAL changes over the Podman network.

### How the primary is configured

The primary service starts PostgreSQL with these settings in `compose.yml`:

```text
wal_level=replica
max_wal_senders=10
max_replication_slots=10
hot_standby=on
```

- `wal_level=replica` makes PostgreSQL write enough WAL information for physical replicas.
- `max_wal_senders=10` allows up to ten server processes to send WAL. This example needs two.
- `max_replication_slots=10` reserves capacity for replication slots. The demo does not create slots, but the setting leaves room for them.
- `hot_standby=on` permits a replica to accept read-only queries while it replays WAL.

The primary health check runs `pg_isready` against the `ryow` database. Replicas depend on that health check, so they do not begin their base backup until the primary is accepting connections.

`00-primary-init.sh` creates two roles on the first primary startup:

- `replicator` can log in and use the PostgreSQL replication protocol.
- `ryow` is the application login used by Spring JDBC.

The script also appends two local-demo rules to `pg_hba.conf`. One permits the `replicator` role to connect for replication, and the other permits database clients to connect using password authentication. The rules use `0.0.0.0/0` because the containers communicate over a private local Podman network; do not copy this authentication policy unchanged into a production deployment.

`01-schema.sql` creates the `user_social_profile` table and grants the `ryow` role permission to use the public schema and select, insert, and update that table. Because the file uses `CREATE TABLE IF NOT EXISTS`, it is safe to read and rerun during a clean local initialization, although PostgreSQL's image itself invokes it only for a new primary volume.

### How each replica is initialized

Each replica runs `replica-entrypoint.sh` instead of the image's normal entrypoint. The script performs this sequence:

1. Wait for the hostname `primary` on port `5432` to accept connections.
2. Ensure the mounted data directory belongs to PostgreSQL's unprivileged `postgres` user.
3. Remove the empty replica data directory contents.
4. Run `pg_basebackup` against the primary using the `replicator` role.
5. Use `-R` so PostgreSQL writes `standby.signal` and a connection configuration pointing back to `primary`.
6. Start PostgreSQL as the unprivileged `postgres` user.

Once started, the replica log should contain lines similar to:

```text
database system is ready to accept read-only connections
started streaming WAL from primary
```

The replicas publish different host ports, but both listen on port `5432` inside their own containers. The application connects to the host-published ports, while the replication connection uses the internal service name `primary`.

### Start PostgreSQL

From the repository root:

```bash
podman compose -f podman/compose.yml up -d
podman compose -f podman/compose.yml ps
```

The primary is published on `localhost:5432`; the replicas are published on `localhost:5433` and `localhost:5434`. The first startup creates the `ryow` database objects, configures streaming replication, and takes a base backup for each replica. The first startup can take a little longer because each replica waits for the primary to become healthy.

Check the service state:

```bash
podman compose -f podman/compose.yml ps
```

All three services should be running. The primary should show `healthy`. The replicas may not have a Compose health check, so `Up` plus the replication queries below are the meaningful checks for them.

To rebuild the topology from scratch, including the local volumes:

```bash
podman compose -f podman/compose.yml down -v
podman compose -f podman/compose.yml up -d
```

Verify that the primary sees two streaming replicas. With the default project name, the containers are named `podman_primary_1`, `podman_replica-first_1`, and `podman_replica-second_1`:

```bash
podman exec podman_primary_1 \
  psql -U postgres -d ryow \
  -c "select client_addr, state, sync_state from pg_stat_replication order by client_addr;"
```

The result should contain two rows with `state` equal to `streaming`. Confirm that each replica is in standby/recovery mode:

```bash
podman exec podman_replica-first_1 \
  psql -U postgres -d ryow -c "select pg_is_in_recovery();"
podman exec podman_replica-second_1 \
  psql -U postgres -d ryow -c "select pg_is_in_recovery();"
```

Both commands should return `t`. To inspect the startup and WAL streaming messages:

```bash
podman compose -f podman/compose.yml logs --tail=100 replica-first replica-second
```

### Reset the cluster

Initialization scripts run only when the primary data directory is empty. If you change the bootstrap scripts or want to repeat the setup from scratch, remove the named volumes:

```bash
podman compose -f podman/compose.yml down -v
podman compose -f podman/compose.yml up -d
```

The `-v` is important here. Without it, PostgreSQL reuses its existing data directory and does not rerun the files under `/docker-entrypoint-initdb.d/`.

If a replica fails during initialization, inspect its logs first. A common symptom is an old or partially initialized replica volume; `down -v` followed by `up -d` removes that stale state.

## Start the application

```bash
./mvnw spring-boot:run
```

The default connection settings are:

| Connection | JDBC URL | Purpose |
| --- | --- | --- |
| `readWrite` | `jdbc:postgresql://localhost:5432/ryow` | Inserts and updates; recent RYOW reads |
| `readOnlyFirst` | `jdbc:postgresql://localhost:5433/ryow` | Ordinary reads, first replica |
| `readOnlySecond` | `jdbc:postgresql://localhost:5434/ryow` | Ordinary reads, second replica |

Override these with `RYOW_PRIMARY_URL`, `RYOW_REPLICA_FIRST_URL`, `RYOW_REPLICA_SECOND_URL`, `RYOW_DB_USERNAME`, and `RYOW_DB_PASSWORD`. The one-minute window can be changed with `RYOW_WINDOW`, using an ISO-8601 duration such as `PT30S`.

## API

Create a profile. Any incoming `X-RYOW-header` is accepted but deliberately ignored on writes:

```bash
curl -i -X POST http://localhost:8080/api/profiles \
  -H 'Content-Type: application/json' \
  -H 'X-RYOW-header: ignored-on-write' \
  -d '{
    "username":"ada",
    "email":"ada@example.com",
    "firstName":"Ada",
    "lastName":"Lovelace",
    "dateOfBirth":"1815-12-10",
    "country":"United Kingdom",
    "city":"London",
    "bio":"Mathematician and author",
    "interests":"computing, mathematics, music",
    "occupation":"Mathematician",
    "education":"University of London",
    "relationshipStatus":"SINGLE",
    "privacyLevel":"PUBLIC",
    "preferredLanguage":"en",
    "timezone":"Europe/London"
  }'
```

Copy the profile UUID from the `Location` header and the epoch-millisecond value from `X-RYOW-header`.

Update a profile:

```bash
curl -i -X PUT http://localhost:8080/api/profiles/<UUID> \
  -H 'Content-Type: application/json' \
  -H 'X-RYOW-header: ignored-on-write' \
  -d '{"username":"ada","email":"ada@example.com","firstName":"Ada","lastName":"Lovelace","bio":"Updated biography","privacyLevel":"PUBLIC","preferredLanguage":"en","timezone":"Europe/London"}'
```

Read immediately with the write token:

```bash
curl -i http://localhost:8080/api/profiles/<UUID> \
  -H 'X-RYOW-header: <epoch-milliseconds-from-write-response>'
```

This returns `X-RYOW-Source: readWrite`. A read without the header, or with a token at least 60 seconds old, alternates between `readOnlyFirst` and `readOnlySecond`.

## Timestamp policy

The write response token is the UTC Unix epoch timestamp in milliseconds captured after the application has selected the successful write operation. It is intentionally not generated with `System.nanoTime()`: `nanoTime()` is monotonic but process-relative and cannot be meaningfully returned to and interpreted by a client. Epoch milliseconds provide a portable HTTP value. A future token is conservatively routed to the primary; malformed tokens lose RYOW affinity and follow normal replica routing.

This is a teaching implementation. In a multi-application-node deployment, use a shared clock discipline and consider a signed token if clients are not trusted to supply the header honestly.

## Scope of this phase

The core routing and PostgreSQL topology are intentionally implemented without JPA/Hibernate, Flyway, Testcontainers, or automated test cases. The schema is an idempotent SQL bootstrap for the local Podman setup. Those pieces can be added in a later phase without changing the API or routing contract.