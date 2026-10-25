# Chapter 8: Entities, Repositories, and CRUD Persistence

Open this complete project in VS Code using **File > Open Folder** and select
the folder containing `pom.xml`. You need JDK 25, VS Code Java support, and
PostgreSQL 18. The [database guide](database/ch08/README.md) provides an isolated
local PostgreSQL route on port 5433, separate from Chapter 7, with Docker as an
optional alternative. Bruno or curl supplies API requests.
The Maven Wrapper supplies Maven 3.9.16, not Java. First builds need internet
access to Maven repositories. No textbook repository or history is required.

This project exports historical Northstar milestone `912ca14` with reviewed
Chapter 6 boundary corrections and Chapter 8 persistence corrections. Product
uses PostgreSQL. Category is a scalar ID, not an ORM relationship yet. There is
no authentication or authorization. Keep the application on localhost.

## Setup, build, and run

Follow the database guide first. The full test suite needs its own disposable
database with the original seed rows. Never point it at retained student work
or shared data. Check `java -version` and `./mvnw --version` before building.

```bash
./mvnw clean verify
java -jar target/northstar-market-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1
```

On PowerShell use `.\mvnw.cmd clean verify`; the Java command is the same.
Keep the terminal open. Use `--server.port=8090` if 8080 is occupied and change
the client URLs accordingly. Stop with Ctrl+C and confirm the listener closes.
Restarting Java preserves rows. Removing database storage does not.

The reviewed suite contains 49 tests, including nine PostgreSQL persistence
tests. Expected constraint violations appear in logs; inspect the final test
summary. A green controller slice does not prove database behavior.

## Requests and contract

```bash
curl -i http://127.0.0.1:8080/api/products/42
curl -i -H 'Content-Type: application/json' -d '{"name":"Restart-Proof Lantern","description":null,"price":1.2300}' http://127.0.0.1:8080/api/products
```

In PowerShell use `Invoke-RestMethod` for JSON, or `curl.exe` with a saved JSON
file and `--data-binary @request.json` to avoid shell quoting differences.
Capture the actual created ID and Location header. Never assume it is 43:
rolled-back tests consume sequence values. Restart Java and GET that same ID.

The `bruno-ch05` and `bruno-ch06` folders are self-contained collections.
Open them in Bruno and select Local. For an installed CLI, run from each folder:

```bash
bru run --env Local --env-var baseUrl=http://127.0.0.1:8080 --bail
```

Run the Chapter 5 collection on a dedicated freshly seeded database. Its list
assertions assume that state; restarting Java no longer resets it. The canonical
stage contract is `contract/northstar-product-api.yaml`. Generated documentation
at `/v3/api-docs` and `/swagger-ui/index.html` needs comparison with that contract.
`/api/status` is a fixed teaching value. `/actuator/health` includes the database
health check but does not establish deployment readiness.

## Lecture files and focused checks

All Java paths below begin at `src/main/java/com/example/northstarmarket/product/`.

| Demonstration | Files | Evidence |
| --- | --- | --- |
| Migration and mapping | `ProductEntity.java`, `CategoryEntity.java`, `src/main/resources/db/migration/`, `application.properties` under `src/main/resources` | Flyway versions 1, 2, 2.1; successful Hibernate validation |
| Managed replacement | `JpaProductCatalog.java`, `ProductRepository.java` | Select/update SQL, changed row, unchanged ID |
| Restart survival | Product POST/GET/PUT/DELETE | Same values after a new Java process, then 204 and 404 |
| Boundary tests | `src/test/java/com/example/northstarmarket/product/ProductPersistenceIntegrationTests.java` and `PersistenceBoundaryRegressionTests.java` | Constraint, identity, dirty checking, Unicode, and paging evidence |

```bash
./mvnw -Dtest=ProductPersistenceIntegrationTests#managedReplacementIsWrittenThroughDirtyChecking test
./mvnw -Dtest=PersistenceBoundaryRegressionTests test
```

Use `.\mvnw.cmd` in PowerShell. The in-memory catalog and appointment fixtures
exist only in test sources. They do not provide live endpoints or runtime storage.
Set `NORTHSTAR_SHOW_SQL=true` only for local diagnosis. Do not add bind-value
logging or record credentials. Rebuild the JAR after editing source.

## Existing Chapter 7 data

Use the optional transfer procedure in the database guide. It copies into a
separate fresh Chapter 8 database and leaves the Chapter 7 source unchanged.
It does not rewrite applied migrations, auto-baseline an unknown schema, or
overwrite a destination containing student changes. After importing, use a
separate database for seed-dependent automated tests.

## Reset and distribution limits

For a repeatable lab, create another isolated database/container and update the
connection variables. Do not reset an existing database to make tests pass.
The database guide distinguishes stopping from deleting owned disposable data.
Keep passwords, CSV exports, `target`, logs, and editor state out of Git.

The reviewed macOS run used PostgreSQL 18.6 and JDK 25. Windows and DBeaver were
not executed. Runtime checks used an alternative HTTP verifier; the Bruno CLI
was not run. This teaching project has no security approval
or license grant for a new public repository; the instructor chooses those terms.
