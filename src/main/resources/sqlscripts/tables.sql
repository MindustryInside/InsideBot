begin;

create table if not exists guild_config(
    id bigint not null default next_id()
        constraint guild_config_pkey
            primary key,
    guild_id bigint not null,
    timezone varchar(255) not null,
    locale varchar(255) not null,
    prefixes jsonb not null
);

create unique index if not exists guild_config_guild_id_idx
    on guild_config(guild_id);

create table if not exists activity(
    id bigint not null default next_id()
        constraint activity_pkey
            primary key,
    guild_id bigint not null,
    user_id bigint not null,
    message_count int not null,
    last_sent_message timestamptz
);

create unique index if not exists activity_guild_id_user_id_idx
    on activity(guild_id, user_id);

create table if not exists activity_config(
    id bigint not null default next_id()
        constraint activity_config_pkey
            primary key,
    guild_id bigint not null,
    enabled bool not null,
    role_id bigint not null,
    message_threshold int not null,
    counting_interval interval not null
);

create unique index if not exists activity_config_guild_id
    on activity_config(guild_id);

create table if not exists reaction_role(
    id bigint not null default next_id()
        constraint reaction_role_pkey
            primary key,
    guild_id bigint not null,
    message_id bigint not null,
    role_id bigint not null,
    emoji jsonb not null
);

create index if not exists reaction_role_guild_id_message_id
    on reaction_role(guild_id, message_id);

create unique index if not exists reaction_role_guild_id_message_id_role_id
    on reaction_role(guild_id, message_id, role_id);

create table if not exists starboard(
    id bigint not null default next_id()
        constraint starboard_pkey
            primary key,
    guild_id bigint not null,
    source_message_id bigint not null,
    target_message_id bigint not null
);

create unique index if not exists starboard_guild_id_source_message_id
    on starboard(guild_id, source_message_id);

create table if not exists starboard_config(
    id bigint not null default next_id()
        constraint starboard_config_pkey
            primary key,
    guild_id bigint not null,
    enabled bool not null,
    threshold int not null,
    starboard_channel_id bigint not null,
    emojis jsonb not null,
    self_starring bool not null
);

create unique index if not exists starboard_config_guild_id
    on starboard_config(guild_id);

create table if not exists moderation_config(
    id bigint not null default next_id()
        constraint moderation_config_pkey
            primary key,
    guild_id bigint not null,
    enabled bool not null,
    warn_expire_interval interval null,
    mute_base_interval interval null,
    threshold_punishments jsonb null,
    admin_role_ids jsonb null,
    mute_role_id bigint null,
    ping_spam_threshold int null
);

create unique index if not exists moderation_config_guild_id
    on moderation_config(guild_id);

create type moderation_action_type as enum ('warn', 'mute');

create table if not exists moderation_action(
    id bigint not null default next_id()
        constraint moderation_action_pkey
            primary key,
    guild_id bigint not null,
    admin_id bigint not null,
    target_id bigint not null,
    type moderation_action_type not null,
    reason varchar(512) null,
    end_timestamp timestamptz null
);

create index if not exists moderation_action_guild_target_id_type
    on moderation_action(guild_id, target_id, type);

commit;
