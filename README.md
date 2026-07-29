# Distributed Trace Intelligence Platform

A backend that ingests distributed traces and explains **why a request failed**
and **where its latency went**. See `Distributed_Trace_Intelligence_Platform.md`
for the full build plan and `user_system_flow.md` for the flows.

This repo is **milestone 1**: the dependency graph engine and the failure RCA
engine, as a pure-Java core with no framework. The latency engine, the HTTP
layer, persistence, and the demo page come next.

## What works now

- **Graph construction** — two-pass build of a dependency tree from a flat span
  list, with strict validation (unknown parent, duplicate id, multiple/zero roots).
- **Failure RCA** — dependency-aware DFS that returns *all* deepest-failing
  services (so independent failures aren't collapsed), separates cascade victims
  from true causes, and derives a confidence level from the failure's shape.
- **Trace parsing** — Jackson JSON → domain objects, isolated to one class so
  the analysis core stays dependency-free.

## Layout

```
com.trace.model    SpanDTO, SpanNode, TraceGraph, SpanStatus   (pure, no deps)
com.trace.graph    TraceGraphBuilder, InvalidTraceException
com.trace.rca      FailureRcaEngine, RcaResult, Confidence
com.trace.ingest   TraceParser, ParsedTrace                    (only Jackson user)
com.trace.Demo     load a trace -> build graph -> print RCA
```

## Run

```bash
mvn test                                         # unit tests (graph + RCA)
mvn -q compile exec:java -Dexec.mainClass=com.trace.Demo
mvn -q compile exec:java -Dexec.mainClass=com.trace.Demo -Dexec.args="demo/latency-payment.json"
```

The default demo trace is a four-service failure cascade; expect
`Root cause: database`.

## Trace format

```json
{
  "traceId": "checkout-1",
  "spans": [
    { "spanId": "s1", "parentSpanId": null, "serviceName": "api-gateway",
      "startTime": 0, "duration": 8000, "status": "OK" },
    { "spanId": "s2", "parentSpanId": "s1", "serviceName": "payment-service",
      "startTime": 200, "duration": 6200, "status": "OK", "attributes": {} }
  ]
}
```

Times are milliseconds. `status` is `OK` or `ERROR`. Unknown fields are ignored.
