begin;

create unique index if not exists idx_strbd_gi_smi
    on starboard(guild_id, source_message_id);

commit;
