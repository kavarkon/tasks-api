[![Gradle build](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml/badge.svg)](https://github.com/kavarkon/tasks-api/actions/workflows/main.yml) CI runs `./gradlew build` to assemble the application and run the test suite.

Public API: `https://todo.puorp.ru/api`. Check it without signing in:

```bash
curl https://todo.puorp.ru/api/auth/csrf
```

## Develop the frontend against the public API

```bash
cd ~/project/tasks-front
npm ci
npm run dev
```

Open `http://localhost:5173`. Use relative paths such as `/api/packs`. Vite proxies them to the public API, so no local Java, Docker or PostgreSQL is needed. Use a separate development account: registrations, tasks and edits go to the production database.

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

### Release the image
From GitHub Actions, run [Release Docker image](.github/workflows/release.yml) manually on `main`. It runs the Gradle tests, builds the image and checks it with the `prod` profile against PostgreSQL before pushing it to Docker Hub. The image is tagged with the Git commit. Copy the image reference with its digest from the workflow summary. Publishing does not deploy to `artist`.

### Update artist
In the `nixos-server` repository, replace the image digest for `tasks-api` in `hosts/artist.nix` with the one from the release. From the repository root, apply the configuration:

```bash
nixos-rebuild switch --flake .#artist --target-host root@artist --no-reexec
```

On `artist`, Nix sets `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `SPRING_DATASOURCE_USERNAME` and `CORS_ALLOWED_ORIGIN`. An agenix-encrypted environment file supplies `SPRING_DATASOURCE_PASSWORD` and `JWT_SECRET`. Flyway applies pending migrations to the existing database during startup. A normal update does not require a database reset.

### Run the JAR manually
Set `DATABASE_URL`, `CORS_ALLOWED_ORIGIN` and `JWT_SECRET`. If database credentials are not in the JDBC URL, also set `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Generate the signing key once, for example with `openssl rand -hex 32`. Store it securely and supply the same value across restarts. JWTs expire after seven days by default. Set `JWT_EXPIRATION_SECONDS` to change the lifetime.

With those variables exported, build the executable JAR and start it with the `prod` profile:

```bash
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/app.jar
```

As in tests, Flyway applies migrations and Hibernate validates the schema.

## API

* [API contract](doc/contract.md): endpoints, authentication, CSRF, request fields, response formats and errors.
* [API scenarios](doc/scenarios.md): examples of signing in, moving a task between packs and reading lists one page at a time.
