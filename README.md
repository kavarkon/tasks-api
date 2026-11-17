[![Java CI with Gradle](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml/badge.svg)](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml)

# Tasks API

Small REST API

## Requirements

- Java 21
- PostgreSQL for local development
- Docker for integration tests with Testcontainers

## Run locally

The default profile is `dev`. It expects PostgreSQL at `localhost:5432` with database `tasks`, user `postgres` and password `password`.

Set `JWT_SECRET` to a random secret of at least 32 UTF-8 bytes. Keep it private and use the same value across application restarts so existing tokens remain valid. For example, generate a local development secret with:

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
```

```bash
./gradlew bootRun
```

To use another database, set `DATABASE_URL`:

```bash
DATABASE_URL='jdbc:postgresql://localhost:5432/tasks?user=postgres&password=password' ./gradlew bootRun
```

In the `dev` profile, Hibernate creates the schema at startup and drops it on a normal shutdown; development data is not preserved. Flyway is disabled in this profile. In the `test` and `prod` profiles, Flyway manages the schema and Hibernate validates it.

## Profiles

- `dev` is for local development and uses `DATABASE_URL` or the local PostgreSQL default.
- `test` is used by integration tests and receives its database settings from Testcontainers.
- `prod` requires `DATABASE_URL` and `CORS_ALLOWED_ORIGIN` and validates the existing schema.

All profiles require `JWT_SECRET`. JWTs expire after seven days by default; set `JWT_EXPIRATION_SECONDS` to change this. `CORS_ALLOWED_ORIGIN` specifies the frontend origin allowed to make requests with cookies. It defaults to `http://localhost:5173` in the `dev` profile; set it explicitly to the production frontend origin in `prod`.

Run with a profile explicitly:

```bash
SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL='jdbc:postgresql://db/tasks?user=app&password=secret' \
  CORS_ALLOWED_ORIGIN='https://frontend.example.com' \
  java -jar build/libs/tasks-0.0.1-SNAPSHOT.jar
```

The example assumes `JWT_SECRET` is already set in the environment. Replace the example database credentials and frontend origin with the actual production values.

## API

### Authentication and CSRF

- `GET /api/auth/csrf` returns a CSRF cookie and `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`.
- `POST /api/auth/register` with `{ "email": "user@example.com", "password": "example-password" }` creates a user but does not log them in.
- `POST /api/auth/login` with the same fields returns the user and sets an `HttpOnly` `access_token` cookie. In production the cookie also has `Secure` and `SameSite=Lax`.
- `POST /api/auth/logout` clears the access-token cookie and returns `204 No Content`. A copied JWT remains valid until it expires.

Before sending a `POST`, `PUT`, `PATCH`, or `DELETE` request, including registration, login, and logout, get a CSRF token and send both the CSRF cookie and the returned token in the returned header. After a successful login or logout, get a new CSRF token. For cross-origin browser requests, include cookies, for example with `credentials: "include"` in `fetch`.

### Packs

- `GET /api/packs`
- `GET /api/packs/{id}` returns a pack with its tasks ordered by ID ascending
- `POST /api/packs` with `{ "title": "Work" }`
- `PUT /api/packs/{id}` with `{ "title": "Updated work" }`
- `DELETE /api/packs/{id}`

### Tasks

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks` with `{ "title": "Read", "completed": false, "packId": 1 }`
- `PUT /api/tasks/{id}` with `{ "title": "Read book", "completed": true, "packId": 1 }`
- `DELETE /api/tasks/{id}`

Pack and task operations require authentication and are limited to the current user's resources. Unknown resources and resources belonging to another user return `404 Not Found`. Creation returns `201 Created` with the created resource and a `Location` header. Successful deletion returns `204 No Content`.

Errors contain `status`, `message`, and `path`. Validation errors return `400 Bad Request` and include a `fields` object with messages for invalid fields. Invalid request bodies or IDs also return `400 Bad Request`.

## Tests

```bash
./gradlew test
```

The tests start PostgreSQL through Testcontainers, so Docker must be running.
