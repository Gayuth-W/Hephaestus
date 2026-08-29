# Hephaestus — Distributed Trace Intelligence Platform

Analyzes distributed traces and explains **why a request failed** (dependency-aware
root cause analysis) and **where its latency went** (interval-union self-time).
Backend at the repo root; the Angular UI is in `frontend/`.

## Auth model

Self-service email + password. Anyone can register; there are no roles — every
signed-in user can analyze traces and manage **their own** reports (reports are
scoped to the user who created them).

- `POST /api/auth/register` — `{ "email", "password" }` → `{ "token", "email" }`
- `POST /api/auth/login` — `{ "email", "password" }` → `{ "token", "email" }`
- `POST /api/traces/analyze` — analyze a trace (saved as yours)
- `GET  /api/reports` — your incident history
- `GET  /api/reports/{id}` — one of your reports
- `DELETE /api/reports/{id}` — delete one of your reports

All `/api/*` except `/api/auth/**` require `Authorization: Bearer <token>`.

## Stack

Java 17 · Spring Boot 3.3.5 · PostgreSQL (JSONB) · Flyway · Spring Security + JWT
(DB-backed users) · JUnit 5 + Testcontainers · Docker Compose · GitHub Actions ·
Angular 18 (frontend)

## Run the backend

```bash
docker compose up --build      # app on :8080, Postgres on host :5433, Flyway migrates on boot
```

Then register and use it:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"secret1"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/traces/analyze \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  --data @demo/failure-cascade.json
```

Postgres is on host port **5433** (so it won't clash with a local Postgres on 5432);
override with `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`. To run the app from Maven instead:
`docker compose up -d db` then `mvn spring-boot:run`.

## Run the frontend

```bash
cd frontend
npm install
npm start                      # http://localhost:4200, proxies /api -> :8080
```

Open http://localhost:4200, create an account, paste a trace, and get the
dependency graph + latency timeline. Needs the backend running on :8080.

## Tests

```bash
mvn test        # unit tests + a Testcontainers integration test (register -> analyze -> report)
```

The integration test starts its own Postgres, so **Docker must be running**.

## Trace format

```json
{ "traceId": "checkout-1",
  "spans": [
    { "spanId":"s1","parentSpanId":null,"serviceName":"api-gateway","startTime":0,"duration":8000,"status":"OK" },
    { "spanId":"s2","parentSpanId":"s1","serviceName":"payment-service","startTime":200,"duration":6200,"status":"OK" }
  ] }
```

Times are milliseconds; `status` is `OK` or `ERROR`.
