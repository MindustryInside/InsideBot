    create sequence if not exists seq_id;

create or replace function next_id(out result bigint) as
$$
declare
    -- Дата взята с id бота
    our_epoch bigint := 1598361056177;
    now_millis bigint;
    seq_id bigint;
begin
    select nextval('seq_id') % 1024 into seq_id;
    select floor(extract(epoch from clock_timestamp()) * 1000) into now_millis;
    result := (now_millis - our_epoch) << 10;
    result := result | seq_id;
end;
$$ language plpgsql;
