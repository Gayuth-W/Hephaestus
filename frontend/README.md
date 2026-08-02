# Hephaestus UI

Angular frontend for the Hephaestus trace-intelligence backend. Sign in, paste a
distributed trace, and see the failure root cause or latency sink rendered as a
dependency graph + timeline.

## Prerequisites

- Node 18.19+ (built on Node 22)
- The **Hephaestus backend running on `http://localhost:8080`** (it's secured, so
  the UI logs in first)

## Run

```bash
npm install
npm start            # ng serve on http://localhost:4200
```

`npm start` proxies `/api/*` to `localhost:8080` (see `proxy.conf.json`), so there
are no CORS issues in development. Open http://localhost:4200 and sign in with the
backend's demo credentials — `admin` / `admin` (ADMIN) or `viewer` / `viewer`
(VIEWER). Analyzing traces needs the ADMIN role.

## Build

```bash
npm run build        # output in dist/
```

For production you'd serve the built `dist/` behind the same origin as the API
(or drop it into the backend's `src/main/resources/static`) so no proxy/CORS is
needed.

## Layout

```
src/app/
  api.service.ts        login + analyze calls, holds the JWT
  models.ts             response types
  app.component.*       shell + login gate
  analyzer.component.*  trace input, SVG dependency graph, latency timeline
```
