package inside.data.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.ResettableInterval;
import inside.data.api.r2dbc.DatabaseClient;
import inside.data.api.r2dbc.R2dbcConnection;
import inside.data.api.r2dbc.R2dbcResult;
import inside.data.api.r2dbc.R2dbcStatement;
import inside.data.schedule.Trigger.State;
import inside.util.Preconditions;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.Result;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static reactor.function.TupleUtils.consumer;
import static reactor.function.TupleUtils.function;

public class ReactiveSchedulerImpl implements ReactiveScheduler {
    protected static final Logger log = Loggers.getLogger(ReactiveSchedulerImpl.class);
    protected static final Logger misfireHandlerLog = Loggers.getLogger(
            ReactiveSchedulerImpl.class.getPackageName() + ".MisfireHandler");
    protected static final AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());
    protected static final Duration misfireDuration = Duration.ofSeconds(15);
    protected static final int maxToRecoverAtATime = 20;

    protected final DatabaseClient client;
    protected final SchedulerResources resources;

    protected final ObjectMapper objectMapper;
    protected final JobFactory jobFactory;
    protected final List<TriggerPersistenceDelegate> triggerPersistenceDelegates = new LinkedList<>();

    public ReactiveSchedulerImpl(DatabaseClient client, SchedulerResources resources,
                                 ObjectMapper objectMapper, JobFactory jobFactory) {
        this.client = Objects.requireNonNull(client, "client");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.jobFactory = Objects.requireNonNull(jobFactory, "jobFactory");

        triggerPersistenceDelegates.add(new SimpleTriggerPersistenceDelegate(resources.getInstanceName()));
    }

    @Override
    public SchedulerResources resources() {
        return resources;
    }

    @Override
    public JobFactory jobFactory() {
        return jobFactory;
    }

    @Override
    public Mono<Void> start() {
        Scheduler sch = Schedulers.newParallel("inside-scheduler", 3);
        ResettableInterval misfireHandler = new ResettableInterval(sch);
        ResettableInterval scheduleInterval = new ResettableInterval(Schedulers.single());

        misfireHandler.start(Duration.ZERO, misfireDuration);
        scheduleInterval.start(Duration.ZERO, resources.getIdleWaitDuration());

        Mono<Void> misfireHandlerFuture = misfireHandler.ticks()
                .doOnNext(tick -> misfireHandlerLog.debug("Scanning for misfires..."))
                .flatMap(tick -> client.transactional(connection -> countMisfiredTriggersInState(connection,
                                State.waiting, Instant.now().minus(misfireDuration))
                                .filter(i -> i > 0)
                                .switchIfEmpty(Mono.fromRunnable(() -> misfireHandlerLog.debug(
                                        "Found 0 triggers that missed their scheduled fire-time.")))
                                .flatMap(ignored -> recoverMisfiredJobs(connection, false))
                                .defaultIfEmpty(false))
                        .withDefinition(IsolationLevel.READ_COMMITTED))
                .onErrorResume(t -> Mono.fromRunnable(() ->
                                misfireHandlerLog.error("Error handling misfires: " + t.getMessage(), t))
                        .thenReturn(false))
                .then();

        Mono<Void> scheduleHandlerFuture = scheduleInterval.ticks()
                .flatMap(tick -> client.transactionalMany(con -> acquireNextTrigger(con,
                                Instant.now().plus(resources.getIdleWaitDuration()),
                                Math.min(getSchedulerPoolSize(), resources.getMaxBatchSize()),
                                resources.getBatchTimeWindow())
                                .flatMap(trigger -> triggerFired(con, trigger)
                                        .onErrorResume(t -> releaseAcquiredTrigger(con, trigger).then(Mono.empty())))
                                .flatMap(bundle -> {

                                    Mono<Void> execute = jobFactory.newJob(bundle)
                                            .map(job -> new JobExecutionContextImpl(this, bundle, job))
                                            .flatMap(context -> {
                                                log.debug("Calling execute on job " + context.jobDetail());

                                                return Mono.from(context.jobInstance().execute(context));
                                            })
                                            .then();

                                    String fireInstanceId = Objects.requireNonNull(bundle.trigger().getFireInstanceId());

                                    return deleteFiredTrigger(con, fireInstanceId)
                                            .and(deleteTriggerAndJob(con, bundle.trigger().key())).then(Mono.defer(() ->
                                            Mono.delay(Duration.between(Instant.now(), bundle.trigger().startTimestamp()))))
                                            .then(execute)
                                            .publishOn(resources.getScheduler());
                                }))
                        .withDefinition(IsolationLevel.READ_COMMITTED))
                .then();

        return Mono.when(misfireHandlerFuture, scheduleHandlerFuture);
    }

    @Override
    public Mono<Void> scheduleJob(JobDetail jobDetail, Trigger trigger) {
        TriggerPersistenceDelegate del = findTriggerPersistenceDelegate(trigger);

        trigger.setJobKey(jobDetail.key());
        trigger.computeFirstFireTimestamp();

        return client.transactional(con -> storeJob(con, jobDetail, false)
                        .and(storeTrigger(con, trigger, del, State.waiting, false))
                        .and(del.storeExtendedTriggerProperties(con, trigger, State.waiting)))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Mono<ImmutableJobDetail> retrieveJob(Key jobKey) {
        return client.transactional(con -> retrieveJob(con, jobKey))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Mono<Boolean> unscheduleJob(Key triggerKey) {
        return unscheduleJobs(List.of(triggerKey));
    }

    @Override
    public Mono<Boolean> unscheduleJobs(List<? extends Key> triggerKeys) {
        Preconditions.requireArgument(!triggerKeys.isEmpty());
        return client.transactional(con -> unscheduleJobs(con, triggerKeys))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Mono<Trigger> retrieveTrigger(Key triggerKey) {
        return client.transactional(con -> retrieveTrigger(con, triggerKey))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    protected Mono<Integer> deleteTriggerAndJob(R2dbcConnection con, Key key) {
        return selectJobKey(con, key)
                .flatMap(jkey -> selectNumTriggersForJob(con, jkey)
                        .filter(i -> i == 0)
                        .switchIfEmpty(deleteTrigger(con, key).then(Mono.empty()))
                        .flatMap(i -> deleteJobDetail(con, jkey)));
    }

    protected Mono<ImmutableKey> selectJobKey(R2dbcConnection conn, Key triggerKey){
        return conn.createStatement("select job_group, job_name from trigger where scheduler_name = $1 and trigger_group = $2 and trigger_name = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, triggerKey.group())
                .bind(2, triggerKey.name())
                .execute()
                .flatMap(result -> result.mapWith(row ->
                        Key.of(row.getRequiredValue(0, String.class),
                                row.getRequiredValue(1, String.class))))
                .singleOrEmpty();
    }

    protected Mono<Long> selectNumTriggersForJob(R2dbcConnection conn, Key jobKey){
        return conn.createStatement("select count(*) from trigger where scheduler_name = $1 and trigger_group = $2 and trigger_name = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, jobKey.group())
                .bind(2, jobKey.name())
                .execute()
                .flatMap(result -> result.mapWith(row -> row.getLong(0)))
                .singleOrEmpty()
                .defaultIfEmpty(0L);
    }

    protected Mono<Integer> deleteJobDetail(R2dbcConnection conn, Key jobKey) {
        return conn.createStatement("delete from job_detail where scheduler_name = $1 and job_group = $2 and job_name = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, jobKey.group())
                .bind(2, jobKey.name())
                .execute()
                .flatMap(Result::getRowsUpdated)
                .singleOrEmpty();
    }

    protected Mono<Integer> deleteTrigger(R2dbcConnection con, Key key) {
        return con.createStatement("delete from trigger where scheduler_name = $1 and trigger_group = $2 and trigger_name = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, key.group())
                .bind(2, key.name())
                .execute()
                .flatMap(Result::getRowsUpdated)
                .singleOrEmpty();
    }

    protected Mono<Void> releaseAcquiredTrigger(R2dbcConnection conn, Trigger trigger) {
        String fireInstanceId = Objects.requireNonNull(trigger.getFireInstanceId());

        return Mono.when(updateTriggerState(conn, trigger.key(), State.blocked, State.acquired),
                deleteFiredTrigger(conn, fireInstanceId));
    }

    protected Mono<Integer> deleteFiredTrigger(R2dbcConnection conn, String fireInstanceId) {
        return conn.createStatement("delete from fired_trigger where scheduler_name = $1 and fire_instance_id = $2")
                .bind(0, resources.getInstanceName())
                .bind(1, fireInstanceId)
                .execute()
                .flatMap(Result::getRowsUpdated)
                .singleOrEmpty();
    }

    protected Mono<TriggerFiredBundle> triggerFired(R2dbcConnection conn, Trigger trigger) {
        return retrieveJob(conn, trigger.jobKey())
                // .onErrorResume(JobPersistenceException.class,
                //         t -> updateTriggerState(conn, bundle.getKey(), ERROR).then(Mono.empty()))
                .flatMap(job -> updateFiredTrigger(conn, trigger, State.executing).thenReturn(job))
                .flatMap(job -> {
                    Instant prevFireTime = trigger.previousFireTimestamp();
                    trigger.triggered();

                    State state = trigger.nextFireTimestamp() == null ? State.complete : State.waiting;
                    var del = findTriggerPersistenceDelegate(trigger);

                    return storeTrigger(conn, trigger, del, state, true)
                            .and(del.storeExtendedTriggerProperties(conn, trigger, state))
                            .thenReturn(new TriggerFiredBundle(job, trigger,
                                    trigger.key().group().equals(ReactiveScheduler.DEFAULT_RECOVERY_GROUP),
                                    Instant.now(), trigger.previousFireTimestamp(),
                                    prevFireTime, trigger.nextFireTimestamp()));
                });
    }

    protected Mono<Integer> updateFiredTrigger(R2dbcConnection conn, Trigger trigger, State state) {
        String fireInstanceId = Objects.requireNonNull(trigger.getFireInstanceId());
        Instant nextFireTimestamp = Objects.requireNonNull(trigger.nextFireTimestamp());

        return conn.createStatement("update fired_trigger set fire_timestamp = $3, schedule_timestamp = $4, fire_instance_state = $5 " +
                        "where scheduler_name = $1 and fire_instance_id = $2")
                .bind(0, resources.getInstanceName())
                .bind(1, fireInstanceId)
                .bind(2, Instant.now())
                .bind(3, nextFireTimestamp)
                .bind(4, state)
                .execute()
                .flatMap(R2dbcResult::getRowsUpdated)
                .singleOrEmpty();
    }

    protected String getFiredTriggerRecordId() {
        return resources.getInstanceName() + '$' + ftrCtr.getAndIncrement();
    }

    protected int getSchedulerPoolSize() {
        return resources.getScheduler() instanceof Scannable s
                ? s.scanOrDefault(Scannable.Attr.CAPACITY, -1) : -1;
    }

    protected Flux<Trigger> acquireNextTrigger(R2dbcConnection conn, Instant noLaterThan,
                                               int maxCount, Duration timeWindow) {
        return selectTriggerToAcquire(conn, noLaterThan.plus(timeWindow), Instant.now().minus(misfireDuration), maxCount)
                .flatMap(triggerKey -> retrieveTrigger(conn, triggerKey))
                .flatMap(nextTrigger -> {
                    Instant nextFireTime = nextTrigger.nextFireTimestamp();
                    if (nextFireTime == null) {
                        return Mono.empty();
                    }

                    nextTrigger.setFireInstanceId(getFiredTriggerRecordId());

                    return updateTriggerState(conn, nextTrigger.key(), State.acquired, State.waiting)
                            .filter(i -> i > 0)
                            .flatMap(i -> insertFiredTrigger(conn, nextTrigger, State.acquired))
                            .thenReturn(nextTrigger);
                });
    }

    protected Mono<Integer> updateTriggerState(R2dbcConnection conn, Key triggerKey,
                                               State newState, State oldState) {
        return conn.createStatement("update trigger set trigger_state = $5 where scheduler_name = $1 and " +
                        "trigger_group = $2 and trigger_name = $3 and trigger_state = $4")
                .bind(0, resources.getInstanceName())
                .bind(1, triggerKey.group())
                .bind(2, triggerKey.name())
                .bind(3, oldState)
                .bind(4, newState)
                .execute()
                .flatMap(Result::getRowsUpdated)
                .singleOrEmpty();
    }

    protected Mono<Integer> insertFiredTrigger(R2dbcConnection conn, Trigger trigger, State state) {
        String fireInstanceId = Objects.requireNonNull(trigger.getFireInstanceId());

        return conn.createStatement("""
                        insert into fired_trigger (scheduler_name, fire_instance_id, trigger_group, trigger_name, fire_timestamp,
                            schedule_timestamp, priority, fire_instance_state)
                            values ($1, $2, $3, $4, $5, $6, $7, $8)
                        """)
                .bind(0, resources.getInstanceName())
                .bind(1, fireInstanceId)
                .bind(2, trigger.key().group())
                .bind(3, trigger.key().name())
                .bind(4, Instant.now())
                .bindOptional(5, trigger.nextFireTimestamp())
                .bind(6, trigger.priority())
                .bind(7, state)
                .execute()
                .flatMap(Result::getRowsUpdated)
                .singleOrEmpty();
    }

    protected Flux<Key> selectTriggerToAcquire(R2dbcConnection conn, Instant noLaterThan, Instant noEarlierThan, int maxCount) {
        return conn.createStatement("""
                        select trigger_group, trigger_name from trigger where scheduler_name = $1 and trigger_state = $2
                            and next_fire_timestamp <= $3 and (misfire_instruction = -1 or (misfire_instruction != -1 and next_fire_timestamp >= $4))
                            order by next_fire_timestamp, priority desc
                        """)
                .bind(0, resources.getInstanceName())
                .bind(1, State.waiting)
                .bind(2, noLaterThan)
                .bind(3, noEarlierThan)
                .fetchSize(maxCount)
                .execute()
                .flatMap(result -> result.mapWith(row ->
                        Key.of(row.getRequiredValue(0, String.class),
                                row.getRequiredValue(1, String.class))));
    }

    protected Flux<ImmutableKey> misfiredTriggersInState(R2dbcConnection conn, State state, Instant timestamp, int count) {
        return conn.createStatement("""
                        select trigger_group, trigger_name from trigger where scheduler_name = $1 and not (misfire_instruction = %s)
                            and next_fire_timestamp < $2 and trigger_state = $3 order by next_fire_timestamp, priority desc
                        """.formatted(Trigger.MISFIRE_IGNORE_POLICY))
                .bind(0, resources.getInstanceName())
                .bind(1, timestamp)
                .bind(2, state)
                .fetchSize(Math.max(count, 0))
                .execute()
                .flatMap(result -> result.mapWith(row ->
                        Key.of(row.getRequiredValue(0, String.class),
                                row.getRequiredValue(1, String.class))));
    }

    protected Mono<Boolean> recoverMisfiredJobs(R2dbcConnection conn, boolean recovering) {
        int misfires = recovering ? -1 : maxToRecoverAtATime;
        return misfiredTriggersInState(conn, State.waiting, Instant.now().minus(misfireDuration), misfires)
                .collectList()
                .filter(list -> !list.isEmpty())
                .zipWhen(list -> Mono.just(list.size() == misfires))
                .doOnNext(consumer((triggerKeys, bool) -> {
                    if (bool) {
                        misfireHandlerLog.info("Handling the first {} triggers that missed their scheduled fire-time. " +
                                        "More misfired triggers remain to be processed.",
                                triggerKeys.size());
                    } else {
                        misfireHandlerLog.info("Handling {} bundle(s) that missed their scheduled fire-time.",
                                triggerKeys.size());
                    }
                }))
                .flatMap(function((triggerKeys, bool) -> Flux.fromIterable(triggerKeys)
                        .flatMap(triggerKey -> retrieveTrigger(conn, triggerKey))
                        .flatMap(trigger -> updateMisfiredTrigger(conn, trigger, State.waiting).thenReturn(trigger))
                        .reduce(Instant.MAX, (earliestNewTime, trig) -> {
                            Instant next = trig.nextFireTimestamp();
                            return next != null && next.isBefore(earliestNewTime) ? next : earliestNewTime;
                        })
                        .map(instant -> bool)))
                .switchIfEmpty(Mono.fromRunnable(() -> misfireHandlerLog.info(
                                "Found 0 triggers that missed their scheduled fire-time."))
                        .thenReturn(false));
    }

    protected Mono<Void> updateMisfiredTrigger(R2dbcConnection conn, Trigger trig, State state) {
        trig.updateAfterMisfire();
        var del = findTriggerPersistenceDelegate(trig);
        State st = trig.nextFireTimestamp() == null ? State.complete : state;

        return storeTrigger(conn, trig, del, st, true)
                .and(del.storeExtendedTriggerProperties(conn, trig, st));
    }

    protected Mono<Long> countMisfiredTriggersInState(R2dbcConnection conn, State state, Instant timestamp) {
        return conn.createStatement("select count(*) from trigger where scheduler_name = $1 and not " +
                        "(misfire_instruction = " + Trigger.MISFIRE_IGNORE_POLICY + ") and next_fire_timestamp < $2 and trigger_state = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, timestamp)
                .bind(2, state)
                .execute()
                .flatMap(result -> result.mapWith(row -> row.getLong(0)))
                .singleOrEmpty();
    }

    protected Mono<Boolean> unscheduleJobs(R2dbcConnection con, List<? extends Key> triggerKeys) {
        R2dbcStatement st = con.createStatement("delete from trigger where scheduler_name = $1 and trigger_group = $2 and trigger_name = $3");

        for (var it = triggerKeys.iterator(); it.hasNext(); ) {
            Key triggerKey = it.next();
            st.bind(0, resources.getInstanceName());
            st.bind(1, triggerKey.group());
            st.bind(2, triggerKey.name());

            if (it.hasNext()) {
                st.add();
            }
        }

        return st.execute()
                .flatMap(R2dbcResult::getRowsUpdated)
                .single()
                .map(i -> i > 0);
    }

    protected Mono<Trigger> retrieveTrigger(R2dbcConnection con, Key triggerKey) {
        return con.createStatement("select trigger_type from trigger where scheduler_name = $1 and trigger_group = $2 and trigger_name = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, triggerKey.group())
                .bind(2, triggerKey.name())
                .execute()
                .flatMap(result -> result.mapWith(row -> row.getRequiredValue(0, String.class)))
                .flatMap(triggerType -> {
                    var de = findTriggerPersistenceDelegate(triggerType);

                    return de.selectTriggerWithProperties(con, triggerKey);
                })
                .cast(Trigger.class)
                .singleOrEmpty();
    }

    protected Mono<ImmutableJobDetail> retrieveJob(R2dbcConnection con, Key jobKey) {
        return con.createStatement("select * from job_detail where scheduler_name = $1 and job_group = $2 and job_name = $3")
                .bind(0, resources.getInstanceName())
                .bind(1, jobKey.group())
                .bind(2, jobKey.name())
                .execute()
                .flatMap(result -> result.mapWith(row -> JobDetail.builder()
                        .key(row.getRequiredValue(1, String.class),
                                row.getRequiredValue(2, String.class))
                        .jobClass(load(row.getRequiredValue(3, String.class)))
                        .jobData(deserializeJson(row.getRequiredValue(4, Json.class)))
                        .build()))
                .single();
    }

    protected Mono<Void> storeJob(R2dbcConnection conn, JobDetail jobDetail, boolean replaceExisting) {
        String className = Objects.requireNonNull(jobDetail.jobClass().getCanonicalName());

        return serializeJson(jobDetail.jobData()).flatMap(jobData -> conn.createStatement("insert into job_detail " +
                        "(scheduler_name, job_group, job_name, job_class, job_data) values ($1, $2, $3, $4, $5)" +
                        (replaceExisting ? " on conflict (scheduler_name, job_group, job_name) " +
                                "do update set job_class = $4, job_data = $5" : ""))
                .bind(0, resources.getInstanceName())
                .bind(1, jobDetail.key().group())
                .bind(2, jobDetail.key().name())
                .bind(3, className)
                .bind(4, jobData)
                .execute()
                .flatMap(R2dbcResult::getRowsUpdated)
                .single()
                .then());
    }

    protected Mono<Void> storeTrigger(R2dbcConnection conn, Trigger trigger, TriggerPersistenceDelegate del,
                                      State state, boolean replaceExisting) {
        return conn.createStatement("insert into trigger " +
                        "(scheduler_name, trigger_group, trigger_name, job_group, job_name, next_fire_timestamp, " +
                        "prev_fire_timestamp, priority, misfire_instruction, trigger_state, " +
                        "trigger_type, start_timestamp, end_timestamp) values (" +
                        "$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)" +
                        (replaceExisting ? " on conflict (scheduler_name, trigger_group, trigger_name) " +
                                "do update set next_fire_timestamp = $6, prev_fire_timestamp = $7, " +
                                "priority = $8, misfire_instruction = $9, trigger_state = $10, " +
                                "trigger_type = $11, start_timestamp = $12, end_timestamp = $13" : ""))
                .bind(0, resources.getInstanceName())
                .bind(1, trigger.key().group())
                .bind(2, trigger.key().name())
                .bind(3, trigger.jobKey().group())
                .bind(4, trigger.jobKey().name())
                .bindOptional(5, trigger.nextFireTimestamp(), Instant.class)
                .bindOptional(6, trigger.previousFireTimestamp(), Instant.class)
                .bind(7, trigger.priority())
                .bind(8, trigger.misfireInstruction())
                .bind(9, state)
                .bind(10, del.getTypeDiscriminator())
                .bind(11, trigger.startTimestamp())
                .bindOptional(12, trigger.endTimestamp(), Instant.class)
                .execute()
                .flatMap(R2dbcResult::getRowsUpdated)
                .singleOrEmpty()
                .then();
    }

    protected TriggerPersistenceDelegate findTriggerPersistenceDelegate(String triggerType) {
        return triggerPersistenceDelegates.stream()
                .filter(delegate -> delegate.getTypeDiscriminator().equalsIgnoreCase(triggerType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find bundle persistence delegate for bundle type: " + triggerType));
    }

    protected TriggerPersistenceDelegate findTriggerPersistenceDelegate(Trigger trigger) {
        return triggerPersistenceDelegates.stream()
                .filter(delegate -> delegate.canHandle(trigger))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find bundle persistence delegate for bundle: " + trigger));
    }

    protected Mono<Json> serializeJson(Map<String, Object> value) {
        return Mono.fromCallable(() -> Json.of(objectMapper.writeValueAsBytes(value)));
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends Job> load(String className) {
        try {
            return (Class<? extends Job>) Class.forName(className);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    protected Map<String, Object> deserializeJson(Json json) {
        try {
            String s = json.asString();
            if (s.isBlank()) {
                return Map.of();
            }
            return objectMapper.readerForMapOf(Object.class).readValue(s);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }
}
