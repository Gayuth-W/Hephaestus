-- V2 added created_by as a nullable column with no backfill, so any row written
-- before it exists with created_by = NULL. Those rows match no owner: they are
-- invisible to every user and cannot be fetched or deleted through the API.
-- They are orphaned storage that only grows.
--
-- Assign them to a reserved sentinel rather than deleting user data, then make
-- the column NOT NULL so the state cannot recur.
update trace_reports set created_by = 'unclaimed@hephaestus.local' where created_by is null;

alter table trace_reports alter column created_by set not null;

-- The history query filters on created_by and sorts by created_at desc. Two
-- single-column indexes make the database filter and then sort; one composite
-- index in the right order serves both.
drop index if exists idx_trace_reports_created_by;
create index idx_trace_reports_owner_recent on trace_reports (created_by, created_at desc);
