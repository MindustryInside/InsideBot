begin;

create unique index if not exists idx_strbd_gi_smi
    on starboard(guild_id, source_message_id);

create unique index if not exists idx_strbd_gi_tmi
    on starboard(guild_id, target_message_id);

commit;
