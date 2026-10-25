# Chapter 8 database setup and preservation

Use a disposable PostgreSQL 18 environment. Runtime login is `northstar_app`;
migration login is `northstar_migrator`; `northstar_owner` cannot log in.
Do not reuse shared roles or databases without their owner's review.

The historical Chapter 7 directory remains as provenance. For Chapter 8, use
its `admin-bootstrap.sql` only. Do **not** apply its schema plan or run its old
verification helper. Flyway owns the application migrations now.

## Part 1: prepare a separate local server before class

Local PostgreSQL is the course default. Chapter 7's manual schema must remain
separate from Chapter 8's Flyway-managed schema. Use a second PostgreSQL 18
instance on port 5433, leaving Chapter 7 on its existing port, usually 5432.
Roles belong to an entire instance, so a different database name alone does not
isolate the existing Northstar roles. Do not drop roles, change their passwords,
or rerun bootstrap against the Chapter 7 server.

The following macOS/Linux commands initialize a new local lab instance. They
require the PostgreSQL 18 **server tools**, not just psql, on PATH. On Windows,
use an instructor-provisioned separate instance or the optional Docker route.

At the terminal shell, run each command separately:

```bash
psql --version
initdb --version
pg_ctl --version
```

All three must identify PostgreSQL 18. Do not initialize this lab with an older
installation. The commands below work in Bash and Zsh. Use a private directory
outside the project so database files and logs cannot enter the student ZIP:

```bash
export CH08_DATA_DIR="$HOME/.local/share/northstar-ch08-data"
test ! -e "$CH08_DATA_DIR" && test ! -L "$CH08_DATA_DIR"
```

If the second command returns a nonzero status, stop: that location already
exists. Inspect it rather than replacing it. For a repeat session, use the
restart instructions at the end of this guide instead of initializing again.

Only for a new directory, initialize and start the dedicated instance:

```bash
mkdir -p "$HOME/.local/share"
initdb -D "$CH08_DATA_DIR" -U postgres -A scram-sha-256 -W --encoding=UTF8
pg_ctl -D "$CH08_DATA_DIR" -l "$CH08_DATA_DIR/server.log" -o "-p 5433 -h 127.0.0.1" -w start
```

Enter a new local-lab administrator password at initdb's hidden prompts. This
explicitly creates the administrator role `postgres` in the **new** instance.
Your existing server may instead use your OS username, such as `donmiller`.
These are separate accounts on separate servers. Do not run initdb with sudo.
If startup fails because 5433 is occupied, stop and choose an unused port
consistently throughout the guide. Do not stop an unidentified service.

From the Chapter 8 project root, connect to the new instance:

```bash
psql -X -h 127.0.0.1 -p 5433 -U postgres -d postgres
```

At the psql prompt, run the SQL below. It must report PostgreSQL 18 on port 5433
and zero rows for both Northstar name checks:

```sql
SELECT version(), current_user, current_database(), inet_server_port();
SELECT datname FROM pg_database WHERE datname = 'northstar_market';
SELECT rolname FROM pg_roles
WHERE rolname IN ('northstar_owner', 'northstar_migrator', 'northstar_app');
```

If any Northstar name already exists, stop and identify the instance and prior
work before continuing. On the confirmed fresh instance, run the following
psql commands **one at a time**, waiting for the prompt after each command:

```text
\i database/ch07/admin-bootstrap.sql
```

```text
\password northstar_migrator
```

```text
\password northstar_app
```

Use different development passwords. The bootstrap reconnects to
`northstar_market` and creates an empty `northstar` schema. Do not execute any
V1/V2 SQL file manually: Flyway will apply all three migrations during the build.
If any statement fails, stop and inspect the error. Exit psql separately:

```text
\q
```

Privately connect once with a deliberately wrong runtime password, then with
the correct password. The wrong password must fail:

```bash
psql -X -W -h 127.0.0.1 -p 5433 -U northstar_app -d northstar_market -c 'SELECT current_user, current_database();'
```

The correct result is `northstar_app` and `northstar_market`. Do not weaken
authentication if the result differs. Skip the Docker section and continue
with the application environment below.

## Optional alternative: isolated Docker server

Start Docker first. These commands create a new named container, bind PostgreSQL
only to localhost port 5433, and use an anonymous data volume. If the name or port
is occupied, choose a new one consistently. Never remove another lab to continue.

In a **Bash** terminal at the project root:

```bash
read -r -s -p 'Disposable administrator password: ' POSTGRES_PASSWORD
printf '\n'
export POSTGRES_PASSWORD
docker run --detach --name northstar-ch08-lab --publish 127.0.0.1:5433:5432 --env POSTGRES_PASSWORD --env 'POSTGRES_INITDB_ARGS=--auth-host=scram-sha-256 --auth-local=trust' --mount "type=bind,source=$PWD,target=/workspace,readonly" postgres@sha256:1c59e2c3c818eaa0f0628f695b36e7c9e362d6b219b36a54a32df645cbd7e1af
unset POSTGRES_PASSWORD
docker exec northstar-ch08-lab pg_isready -U postgres
```

In **PowerShell**, the equivalent is:

```powershell
$env:POSTGRES_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Disposable administrator password' -AsSecureString)).Password
docker run --detach --name northstar-ch08-lab --publish 127.0.0.1:5433:5432 --env POSTGRES_PASSWORD --env 'POSTGRES_INITDB_ARGS=--auth-host=scram-sha-256 --auth-local=trust' --mount "type=bind,source=$($PWD.Path),target=/workspace,readonly" postgres@sha256:1c59e2c3c818eaa0f0628f695b36e7c9e362d6b219b36a54a32df645cbd7e1af
Remove-Item Env:POSTGRES_PASSWORD
docker exec northstar-ch08-lab pg_isready -U postgres
```

Wait for accepting connections. TCP requires SCRAM, including loopback. The
trusted local administrator socket is a container-lab choice, not a deployment
recommendation. Docker administrators can inspect the initial password in the
container configuration. Use no real service credential or sensitive data.

In either shell, bootstrap once and set two different passwords interactively:

```text
docker exec northstar-ch08-lab psql -X -v ON_ERROR_STOP=1 -U postgres -f /workspace/database/ch07/admin-bootstrap.sql
docker exec -it northstar-ch08-lab psql -X -U postgres -c "\password northstar_app"
docker exec -it northstar-ch08-lab psql -X -U postgres -c "\password northstar_migrator"
```

The bootstrap deliberately fails when roles or the database already exist.
Do not change it to overwrite them silently. For a local PostgreSQL installation,
use a new disposable cluster, apply the same bootstrap with `psql -X -v
ON_ERROR_STOP=1 -f database/ch07/admin-bootstrap.sql` as its administrator, and
set passwords with `\password`. Use that server's actual port below. Confirm
TCP password authentication by checking that a wrong password fails.

## Part 2: configure, migrate, and build before the demos

Supply the same runtime and migration passwords you set above. Bash:

```bash
export NORTHSTAR_DB_URL=jdbc:postgresql://127.0.0.1:5433/northstar_market
export NORTHSTAR_MIGRATION_DB_URL="$NORTHSTAR_DB_URL"
read -r -s -p 'Runtime password: ' NORTHSTAR_DB_PASSWORD
printf '\n'
read -r -s -p 'Migration password: ' NORTHSTAR_MIGRATION_DB_PASSWORD
printf '\n'
export NORTHSTAR_DB_PASSWORD NORTHSTAR_MIGRATION_DB_PASSWORD
```

PowerShell:

```powershell
$env:NORTHSTAR_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/northstar_market'
$env:NORTHSTAR_MIGRATION_DB_URL = $env:NORTHSTAR_DB_URL
$env:NORTHSTAR_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Runtime password' -AsSecureString)).Password
$env:NORTHSTAR_MIGRATION_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Migration password' -AsSecureString)).Password
```

For **Zsh** (the usual macOS terminal), use this block instead of Bash's `read -p`:

```zsh
export NORTHSTAR_DB_URL=jdbc:postgresql://127.0.0.1:5433/northstar_market
export NORTHSTAR_MIGRATION_DB_URL="$NORTHSTAR_DB_URL"
read -r -s 'NORTHSTAR_DB_PASSWORD?Runtime password: '
printf '\n'
read -r -s 'NORTHSTAR_MIGRATION_DB_PASSWORD?Migration password: '
printf '\n'
export NORTHSTAR_DB_PASSWORD NORTHSTAR_MIGRATION_DB_PASSWORD
```

Run each password prompt separately. These variables belong to this terminal
and its child processes, not every terminal in VS Code. Repeat this setup in
any new terminal used for Java or Maven. Never print the password variables.
They remain sensitive to sufficiently privileged local processes. Use these
temporary settings only in the private course lab and unset them afterward.

Run `./mvnw clean verify` or `.\mvnw.cmd clean verify`. Flyway applies 1, 2, and
2.1 before Hibernate validates the model. Inspect history with the migration
identity. Runtime deliberately cannot read Flyway's private table:

```text
psql -X -h 127.0.0.1 -p 5433 -U northstar_migrator -d northstar_market -c "SELECT version,description,success FROM northstar.flyway_schema_history ORDER BY installed_rank;"
```

This host psql command works for both local and Docker routes on port 5433.
If you use Docker without a host psql client, use `docker exec -it
northstar-ch08-lab psql -X -h localhost -U northstar_migrator -d northstar_market`
and run the SELECT at its prompt. Enter the migration password when requested.

**Preparation checkpoint:** expect 49 passing tests, migrations 1/2/2.1 marked
successful, and the packaged JAR. The first full build has already applied V1.
The live lecture begins by inspecting that history, not applying V1 again.

V2.1 corrects prices and Category grants without changing applied V1/V2. It also
supports an existing Flyway V2 database containing valid Products. Invalid prices
cause failure and require review, not automatic deletion. A populated V1-only
database is not an automatic upgrade path through historical V2.

## Optional Chapter 7 transfer

Keep the original Chapter 7 database. Create a **separate** fresh Chapter 8
destination, run migrations and tests, then stop Java and other writers on both
sides. This is an offline teaching transfer, not a production cutover.

Run the helper in Bash with PostgreSQL 18 `psql` on PATH. Windows users can use
Git Bash; this route has not been executed on Windows. URLs are PostgreSQL, not
JDBC, and must name the actual two databases:

If your current shell is Zsh, first enter `bash` on its own line for this optional
procedure. The following `read -p` commands require Bash.

```bash
export CH07_SOURCE_URL=postgresql://northstar_app@127.0.0.1:5432/northstar_market
export CH08_TARGET_URL=postgresql://northstar_migrator@127.0.0.1:5433/northstar_market
read -r -s -p 'Chapter 7 runtime password: ' CH07_SOURCE_PASSWORD
printf '\n'
read -r -s -p 'Chapter 8 migration password: ' CH08_TARGET_PASSWORD
printf '\n'
export CH07_SOURCE_PASSWORD CH08_TARGET_PASSWORD
bash database/ch08/copy-ch07-products.sh
unset CH07_SOURCE_PASSWORD CH08_TARGET_PASSWORD
```

The source must have the pre-Flyway Chapter 7 shape and no Category column.
`previous-ch07-schema.sql` records that prior schema for rehearsal; do not apply
it to the destination. The helper reads a consistent source snapshot and removes
its private temporary CSV afterward. It locks destination tables, requires only
unchanged seed data and history 1/2/2.1, and imports in one transaction.

It preserves IDs and fields, assigns Category 1, and advances the identity
sequence. Source row 42 replaces only the verified disposable destination seed.
If the source has no row 42, the destination seed remains. Invalid IDs/rows or
destination changes cause failure, not a silent skip or merge. It does not
auto-baseline unknown schemas or rewrite migration history.

Compare all transferred fields and counts, accounting for the possible extra
seed. Confirm the original source remains unchanged before switching Java to
the destination. Keep the source until independently verified. Use a separate
disposable database for seed-dependent tests after importing student rows.

## Stop, restart, and deliberate disposal

Ctrl+C stops Java. Leave PostgreSQL running during the restart demonstration.
When finished, clear secrets in every terminal used for Maven or Java:

```bash
unset NORTHSTAR_DB_PASSWORD NORTHSTAR_MIGRATION_DB_PASSWORD NORTHSTAR_SHOW_SQL
```

For the local route, stop only the dedicated instance created above:

```bash
pg_ctl -D "$CH08_DATA_DIR" -m fast -w stop
```

Stopping retains its data. In a later terminal, set `CH08_DATA_DIR` to the same
directory and start it with the same `pg_ctl ... start` command from Part 1.
Do not run initdb or bootstrap again. Restore the application environment before
starting Java. This guide does not ask you to delete local database storage.

For Docker, `docker stop northstar-ch08-lab` preserves this database volume.
`docker start northstar-ch08-lab` starts it again; check readiness before Java.
For a fresh rehearsal, prefer a new container name and port.

Only after confirming this named container is your disposable lab and its data
is no longer needed, `docker rm -v northstar-ch08-lab` after stopping deletes the
container and anonymous volume. That deletion is irreversible without your own
backup. Never substitute a shared name. Unset terminal passwords when finished.

Local server setup references: [PostgreSQL 18 initdb](https://www.postgresql.org/docs/18/app-initdb.html)
and [pg_ctl](https://www.postgresql.org/docs/18/app-pg-ctl.html).
