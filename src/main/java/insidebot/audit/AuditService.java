package insidebot.audit;

import arc.util.Log;
import insidebot.data.repository.MessageInfoRepository;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuditService{
    @Autowired
    private MessageInfoRepository messageInfoRepository;

    @Scheduled(cron = "0 */12 * * *") // каждые 12 часов
    public void cleanUp(){
        messageInfoRepository.getAll().filter(m -> {
            DateTime time = new DateTime(m.timestamp());
            return Weeks.weeksBetween(time, DateTime.now()).getWeeks() >= 4;
        }).subscribe(messageInfoRepository::delete, Log::err);
    }
}
