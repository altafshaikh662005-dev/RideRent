# RideRent-New

RideRent is a Java 17 / Spring Boot 3.3 project with exactly two microservices:

- User Service on `8081`, backed only by `user_db`
- Booking Service on `8082`, backed only by `booking_db`

Booking creation verifies the user through `GET /api/users/{id}` in User Service. Booking Service never connects to `user_db` or shares its tables.

## Prerequisites

- Java 17 or later
- Maven 3.9 or later
- Docker Desktop with Docker Compose
- Jenkins with the Pipeline and Credentials plugins

## Environment Variables

Required for local runs and Compose:

- `DB_PASSWORD`: MySQL root password
- `JWT_SECRET`: shared JWT signing secret of at least 32 characters

Optional variables:

- `DB_USERNAME`: database username, default `root`
- `RATE_LIMIT_REQUESTS_PER_MINUTE`: per-IP API limit, default `60`
- `USER_SERVICE_URL`: Booking Service user URL, default `http://localhost:8081` locally and set to `http://user-service:8081` by Compose

## One-Command Startup

From the `RideRent-New` project directory, run:

```powershell
.\start-riderrent.ps1
```

The script verifies Docker, supplies local-only development values for `DB_PASSWORD` and `JWT_SECRET` when they are not already set, runs `docker compose up -d --build`, waits for both MySQL containers and both services to become healthy, then verifies health, Swagger, JWT authentication, Booking-to-User REST communication, and rate limiting. Secrets and JWTs are never printed. Existing database volumes are preserved.

To stop the application without deleting databases or volumes:

```powershell
docker compose stop
```

To remove the application containers and network while preserving database volumes:

```powershell
docker compose down --remove-orphans
```

## Run With Docker Compose Manually

The startup script is the recommended command. Compose waits for each MySQL health check before starting its service, then waits for User Service before starting Booking Service. To run Compose manually, set `DB_PASSWORD` and `JWT_SECRET`, then use `docker compose up -d --build`. Do not use `docker compose down -v` unless you explicitly want to reset the databases.

## Swagger

- User Service: `http://localhost:8081/swagger-ui/index.html`
- Booking Service: `http://localhost:8082/swagger-ui/index.html`

Swagger is only used for API documentation and testing. It is not an API gateway.

## Authentication

`POST /api/auth/register` and `POST /api/auth/login` are public. Login returns a JWT. Send it to protected user and booking endpoints as:

```text
Authorization: Bearer <JWT>
```

Both services validate the token with the same `JWT_SECRET`.

## Rate Limiting

Each service applies a simple in-memory per-IP limit to `/api/**` requests. Configure it with `RATE_LIMIT_REQUESTS_PER_MINUTE`. Requests above the limit receive HTTP `429 Too Many Requests`; Swagger and health endpoints are not rate limited.

## Jenkins

Create a Jenkins Pipeline job for this repository:

1. Install Jenkins Pipeline, Credentials Binding, Git, and Docker-related plugins.
2. Use a Windows Jenkins agent with Java 17+, Maven, Docker Desktop/Docker Engine, and Docker Compose available on `PATH`.
3. In **Manage Jenkins -> Credentials**, create three Secret text credentials with these exact IDs:
   - `riderrent-db-username`
   - `riderrent-db-password`
   - `riderrent-jwt-secret`
4. Create a **Pipeline** job, choose **Pipeline script from SCM**, select Git, enter the repository URL/credentials, and set the script path to `Jenkinsfile`.
5. Run **Build Now**. The pipeline checks out the source, runs `mvn clean test package`, validates Compose, builds each service image, starts the isolated Compose project, and verifies health, Swagger, JWT, Booking-to-User REST communication, and rate limiting.

The pipeline uses `COMPOSE_PROJECT_NAME=riderrent-jenkins-${BUILD_NUMBER}` so cleanup targets only that build's containers and network. Its `post` cleanup runs `docker compose down --remove-orphans`; it never runs `docker compose down -v`, so persistent database volumes are preserved.

## API Summary

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/{id}`
- `POST /api/bookings`
- `GET /api/bookings/{id}`
- `GET /api/bookings/user/{userId}`
- `PUT /api/bookings/{id}/cancel`
