create table trace_reports (
    id         uuid primary key,
    trace_id   text,
    mode       text,
    created_at timestamptz not null default now(),
    raw_trace  jsonb not null,
    result     jsonb not null
);

create index idx_trace_reports_created_at on trace_reports (created_at desc);
create index idx_trace_reports_trace_id  on trace_reports (trace_id);