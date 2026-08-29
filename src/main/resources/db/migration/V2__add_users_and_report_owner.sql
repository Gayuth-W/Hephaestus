create table users (
    id         uuid primary key,
    email      text not null unique,
    password   text not null,
    created_at timestamptz not null default now()
);

alter table trace_reports add column created_by text;
create index idx_trace_reports_created_by on trace_reports (created_by);
