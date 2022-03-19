begin;

drop table if exists job_detail cascade;
drop table if exists trigger cascade;
drop table if exists simple_trigger cascade;
drop table if exists fired_trigger cascade;
drop type if exists trigger_state cascade;

create table job_detail(
    scheduler_name varchar(200) not null,
    job_group varchar(200) not null,
    job_name varchar(200) not null,
    job_class varchar(250) not null,
    job_data jsonb null,
    primary key (scheduler_name, job_group, job_name)
);

create type trigger_state as enum ('waiting', 'complete', 'acquired', 'executing', 'blocked');

create table if not exists trigger(
    scheduler_name varchar(200) not null,
    trigger_group varchar(200) not null,
    trigger_name varchar(200) not null,
    job_group varchar(200) not null,
    job_name varchar(200) not null,
    next_fire_timestamp timestamptz null,
    prev_fire_timestamp timestamptz null,
    priority integer not null,
    misfire_instruction integer not null,
    trigger_state trigger_state not null,
    trigger_type varchar(200) not null,
    start_timestamp timestamptz not null,
    end_timestamp timestamptz null,
    primary key (scheduler_name, trigger_group, trigger_name),
    foreign key (scheduler_name, job_group, job_name)
        references job_detail(scheduler_name, job_group, job_name)
        on update cascade
        on delete cascade
);

create table simple_trigger(
    scheduler_name varchar(200) not null,
    trigger_group varchar(200) not null,
    trigger_name varchar(200) not null,
    repeat_count bigint not null,
    repeat_interval interval not null,
    times_triggered bigint not null,
    primary key (scheduler_name, trigger_group, trigger_name),
    foreign key (scheduler_name, trigger_group, trigger_name)
        references trigger(scheduler_name, trigger_group, trigger_name)
        on update cascade
        on delete cascade
);

create table fired_trigger(
    scheduler_name varchar(200) not null,
    fire_instance_id varchar(95) not null,
    trigger_group varchar(200) not null,
    trigger_name varchar(200) not null,
    fire_timestamp timestamptz not null,
    schedule_timestamp timestamptz not null,
    priority integer not null,
    fire_instance_state trigger_state not null,
    primary key (scheduler_name, fire_instance_id)
);

end;
