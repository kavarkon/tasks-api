[![Gradle build](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml/badge.svg)](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml) CI runs `./gradlew build` to assemble the application and run the test suite.

## Run locally
The default profile is `dev`. Flyway applies migrations and Hibernate validates the schema. Restarting the API keeps your data. Start with a new empty development database when switching from the old Hibernate-generated schema. Do not reset a database that you need to keep.

### With Docker Compose
From the repository root, build the JAR with JDK 21, then start the API and PostgreSQL 16:

```bash
./gradlew bootJar
docker compose up --build -d
```

Once Spring finishes starting, check the API with `curl http://127.0.0.1:8080/api/auth/csrf`. PostgreSQL is accessible only on the Compose network. The credentials in `compose.yaml` are for local development only. Repeat both commands after changing application code.

* `docker compose down` stops and removes the containers but keeps the database volume. Use `docker compose up -d` to start again with the same data.
* `docker compose down -v` also deletes this project's database volume and all its data. On the next start Flyway creates a fresh schema.

### Without Compose

With Nix, start PostgreSQL first and make sure the `tasks` database exists. The dev shell provides Java 21 and sets `DATABASE_URL` for PostgreSQL on `localhost:5432` with user `postgres` and password `password`. It does not start PostgreSQL.

```bash
./gradlew bootRun
```

Without Nix, use JDK 21 and set `DATABASE_URL` yourself:

```bash
DATABASE_URL='jdbc:postgresql://localhost:5432/tasks?user=postgres&password=password' ./gradlew bootRun
```

The `dev` profile has a development signing key. Set `JWT_SECRET` to override it. The allowed frontend origin defaults to `http://localhost:5173` and can be changed with `CORS_ALLOWED_ORIGIN`.

## Tests
Testcontainers starts PostgreSQL. You need a running container engine accessible to Testcontainers, not a local database or an external JWT secret. The `test` profile uses a dedicated test key. Flyway applies migrations and Hibernate validates the schema.

```bash
./gradlew test
```

Rootless Podman is supported: if `DOCKER_HOST` is unset and the user's Podman socket exists, the dev shell selects it. With JDK 21 and a container engine already configured, run `./gradlew test` directly.

## Production
Set `DATABASE_URL`, `CORS_ALLOWED_ORIGIN` and `JWT_SECRET`. Generate the signing key once, for example with `openssl rand -hex 32`. Store it securely and supply the same value across restarts. JWTs expire after seven days by default. Set `JWT_EXPIRATION_SECONDS` to change the lifetime.

With those variables exported, build the executable JAR and start it with the `prod` profile:

```bash
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/app.jar
```

As in tests, Flyway applies migrations and Hibernate validates the schema.

## API
The `access_token` cookie has `HttpOnly` and `SameSite=Lax` in every profile. In `prod` it also has `Secure`. See [API scenarios](doc/scenarios.md) for the CSRF flow, endpoints, request and response examples, and error formats.
