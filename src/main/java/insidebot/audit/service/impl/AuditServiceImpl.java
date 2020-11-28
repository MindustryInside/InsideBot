package insidebot.audit.service.impl;

import arc.util.Log;
import insidebot.audit.service.AuditService;
import insidebot.data.repository.MessageInfoRepository;
import org.joda.time.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AuditServiceImpl implements AuditService{
    @Autowired
    private MessageInfoRepository messageInfoRepository;

    @Autowired
    private Logger log;

    @Override
    @Scheduled(cron = "0 0 */12 * * *") // каждые 12 часов
    public void cleanUp(){
        Flux.fromIterable(messageInfoRepository.findAll()).filter(m -> {
            DateTime time = new DateTime(m.timestamp());
            return Weeks.weeksBetween(time, DateTime.now()).getWeeks() >= 4;
        }).subscribe(messageInfoRepository::delete, Log::err);

        log.info("Audit cleanup finished");
    }
}
