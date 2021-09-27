begin;

create operator >|(leftarg = jsonb, rightarg = text, procedure = pg_catalog.jsonb_exists);

commit;
