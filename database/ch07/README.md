# Chapter 7 PostgreSQL development environment

Chapter 7 keeps Product API state in memory while establishing the database boundary that Chapter 8 will use. The files in this directory create two distinct database identities, apply the proposed Product schema, and verify privileges and constraints against PostgreSQL 18.

## Roles and ownership

- `northstar_owner` is a `NOLOGIN` role that owns the database objects.
- `northstar_migrator` is a login role allowed to assume `northstar_owner` while applying reviewed migrations.
- `northstar_app` is the runtime login. It can read and change Product rows, but it cannot create or drop schema objects.

An administrator runs `admin-bootstrap.sql` once for a disposable development database. The file deliberately does not contain passwords. Set passwords interactively with `\password northstar_migrator` and `\password northstar_app`, or use the approved secret-management process for your environment.

```bash
psql --dbname postgres --file database/ch07/admin-bootstrap.sql
```

The bootstrap script creates the `northstar_market` database. It is not idempotent because silently changing existing roles or ownership would be unsafe. Use a fresh disposable database cluster for the chapter lab.

## Migration plan

Apply the proposed first version as the migration identity:

```bash
psql "$NORTHSTAR_MIGRATOR_URL" \
  --set ON_ERROR_STOP=1 \
  --file src/main/resources/db/migration/V1__create_product_schema.sql
```

`NORTHSTAR_MIGRATOR_URL` is an environment value such as `postgresql://northstar_migrator@localhost:5432/northstar_market`. Do not place a password in a tracked URL. Let `psql` prompt, use a local password file with restricted permissions, or use the secret mechanism approved for the environment.

The filename follows Flyway's versioned-migration convention. Chapter 7 can apply the SQL directly with `psql`; Chapter 8 lets Flyway discover it from the runtime migration location.

## Application connection preview

Copy the variable names from `application-database.env.example` into an untracked shell or IDE run configuration. The current application does not create a `DataSource` because Product persistence begins in Chapter 8. The values document the future JDBC boundary without placing a credential in source control.

## Verification

Export a runtime connection URL and run the verification script:

```bash
export NORTHSTAR_APP_URL=postgresql://northstar_app@localhost:5432/northstar_market
database/ch07/verify-database.sh
```

The script proves that the runtime role can perform approved row operations, cannot create a table, and cannot insert rows that violate the Product price, scale, name, or length constraints. All row changes occur inside transactions that roll back.
