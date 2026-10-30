[![Java CI with Gradle](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml/badge.svg)](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml)

# Tasks API

Small educational REST API for packs and tasks.

## Requirements

- Java 17
- PostgreSQL for local development
- Docker for integration tests with Testcontainers

## Run locally

The default profile is `dev`. It expects PostgreSQL at `localhost:5432` with database `tasks`, user `postgres` and password `password`.

```bash
./gradlew bootRun
```

To use another database, set `DATABASE_URL`:

```bash
DATABASE_URL='jdbc:postgresql://localhost:5432/tasks?user=postgres&password=password' ./gradlew bootRun
```

Flyway creates the schema from `src/main/resources/db/migration`. Hibernate validates the schema and does not create or drop it automatically.

## Profiles

- `dev` is for local development and uses `DATABASE_URL` or the local PostgreSQL default.
- `test` is used by integration tests and receives its database settings from Testcontainers.
- `prod` requires `DATABASE_URL` and validates the existing schema.

Run with a profile explicitly:

```bash
SPRING_PROFILES_ACTIVE=prod DATABASE_URL='jdbc:postgresql://db/tasks?user=app&password=secret' java -jar build/libs/tasks-api-0.0.1-SNAPSHOT.jar
```

## API

### Packs

- `GET /api/packs`
- `GET /api/packs/{id}` returns a pack with its tasks
- `POST /api/packs` with `{ "title": "Work" }`
- `PUT /api/packs/{id}` with `{ "title": "Updated work" }`
- `DELETE /api/packs/{id}`

### Tasks

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks` with `{ "title": "Read", "completed": false, "packId": 1 }`
- `PUT /api/tasks/{id}` with `{ "title": "Read book", "completed": true, "packId": 1 }`
- `DELETE /api/tasks/{id}`

Creation returns `201 Created` with the created resource and a `Location` header. Successful deletion returns `204 No Content`. Unknown resources return `404 Not Found`, and invalid request bodies or IDs return `400 Bad Request`.

During development, the initial migration may be changed while the database contains no valuable data. Flyway checks migration checksums, so recreate the development database after changing an already applied migration. Once deployed with valuable data, keep applied migrations unchanged and add a new versioned migration for each schema change.

## Tests

```bash
./gradlew test
```

The tests start PostgreSQL through Testcontainers, so Docker must be running.
