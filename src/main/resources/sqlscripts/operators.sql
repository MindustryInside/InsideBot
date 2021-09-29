begin;

-- '?' can't be escaped in jdbc prepared statements
create operator >|(leftarg = jsonb, rightarg = text, procedure = pg_catalog.jsonb_exists);

commit;
