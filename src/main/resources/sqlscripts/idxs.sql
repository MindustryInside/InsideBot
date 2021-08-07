begin;

create unique index on emoji_dispenser(message_id, role_id);

create unique index on local_member(guild_id, user_id);

create unique index on message_info(message_id);

create unique index on starboard(guild_id, source_message_id);

create unique index on starboard(guild_id, target_message_id);;

commit;
