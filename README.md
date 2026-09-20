# RideRent-New

Three-service Java 17 / Spring Boot 3.3 microservices demonstration.

## Services

- API Gateway: `8085`
- User Service: `8081`
- Booking Service: `8082`

The gateway routes `/api/auth/**` and `/api/users/**` to user-service and `/api/bookings/**` to booking-service. Booking creation verifies the user through the user-service REST API; it never accesses `user_db`.

## Environment

Set `DB_USERNAME` (default `root`), `DB_PASSWORD`, and `JWT_SECRET` (at least 32 characters). Optional: `RATE_LIMIT_REQUESTS_PER_MINUTE` (default `10`) and `USER_SERVICE_URL`.

## MySQL setup

```sql
CREATE DATABASE user_db;
CREATE DATABASE booking_db;
```

## Start

Set `DB_PASSWORD` and `JWT_SECRET` once in the Windows User environment, then run `PowerShell -ExecutionPolicy Bypass -File .\start-riderrent.ps1` from this directory. The script inherits those values, uses `root` when `DB_USERNAME` is not set, starts each service in its own PowerShell window, and leaves already-running services alone. You do not need to enter the secrets for each start.

Swagger is available at `http://localhost:8081/swagger-ui.html` and `http://localhost:8082/swagger-ui.html`.

## APIs

`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/{id}`, `POST /api/bookings`, `GET /api/bookings/{id}`, `GET /api/bookings/user/{userId}`, `PUT /api/bookings/{id}/cancel`.
