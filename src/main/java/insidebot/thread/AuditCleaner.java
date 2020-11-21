package insidebot.thread;

import insidebot.data.dao.*;
import org.joda.time.*;

public class AuditCleaner implements Runnable{
    @Override
    public void run(){
        MessageInfoDao.all().filter(m -> {
            DateTime time = new DateTime(m.getTimestamp());
            return Weeks.weeksBetween(time, DateTime.now()).getWeeks() >= 4;
        }).subscribe(MessageInfoDao::remove);
    }
}
