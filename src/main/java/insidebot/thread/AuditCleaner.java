package insidebot.thread;

import arc.util.Log;
import insidebot.data.repository.MessageInfoRepository;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditCleaner implements Runnable{
    @Autowired
    private MessageInfoRepository messageInfoRepository;

    @Override
    public void run(){
        messageInfoRepository.getAll().filter(m -> {
            DateTime time = new DateTime(m.timestamp());
            return Weeks.weeksBetween(time, DateTime.now()).getWeeks() >= 4;
        }).subscribe(messageInfoRepository::delete, Log::err);
    }
}
