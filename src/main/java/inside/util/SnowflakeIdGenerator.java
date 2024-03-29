package inside.util;

public class SnowflakeIdGenerator{

    protected final long epoch;
    protected final long workerId;
    protected final long processId;
    protected long lastTimestamp = -1L;
    protected long sequence = 0;

    public SnowflakeIdGenerator(long epoch, long workerId, long processId){
        Preconditions.requireArgument(workerId <= 31 && workerId >= 0,
                "Worker Id can't be greater than 31 or less than 0");
        Preconditions.requireArgument(processId <= 31 && processId >= 0,
                "Process Id can't be greater than 31 or less than 0");
        this.epoch = epoch;
        this.workerId = workerId;
        this.processId = processId;
    }

    public synchronized long nextId(){
        long timestamp = System.currentTimeMillis();
        if(timestamp < lastTimestamp){
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " +
                    (lastTimestamp - timestamp) + " milliseconds");
        }

        if(lastTimestamp == timestamp){
            sequence = sequence + 1 & 4095L;
            if(sequence == 0){
                timestamp = awaitNextMilli(lastTimestamp);
            }
        }else{
            sequence = 0;
        }

        lastTimestamp = timestamp;
        return timestamp - epoch << 22 |
                workerId << 17 |
                processId << 12 |
                sequence;
    }

    private long awaitNextMilli(long lastTimestamp){
        long timestamp = System.currentTimeMillis();
        while(timestamp <= lastTimestamp){
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
