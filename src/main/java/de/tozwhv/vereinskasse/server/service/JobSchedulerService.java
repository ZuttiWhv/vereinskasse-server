package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.modell.TransactionType;
import de.tozwhv.vereinskasse.server.modell.systemjob.ScheduledFeeJob;
import de.tozwhv.vereinskasse.server.modell.systemjob.SystemJob;
import de.tozwhv.vereinskasse.server.repository.JobRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobSchedulerService {

    private final TaskScheduler taskScheduler;
    private final JobRepository jobRepository;
    private final AccountingService accountingService;
    private final UserRepository userRepository;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // Initialisierung beim Start der Anwendung
    @EventListener(ContextRefreshedEvent.class)
    public void initScheduler() {
        refreshAllJobs();
    }

    public void refreshAllJobs() {
        // Bestehende Tasks stoppen
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();

        // Alle aktiven Jobs neu einplanen
        jobRepository.findByEnabledTrue().forEach(this::scheduleJob);
    }

    private void scheduleJob(SystemJob job) {
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeJob(job),
                new CronTrigger(job.getCronExpression())
        );
        scheduledTasks.put(job.getId(), future);
        log.info("Job {} (Typ: {}) geplant mit Cron: {}", job.getId(), job.getJobType(), job.getCronExpression());
    }

    private void executeJob(SystemJob job) {
        if (job instanceof ScheduledFeeJob feeJob) {
            executeScheduledFeeJob(feeJob);
        }
    }

    private void executeScheduledFeeJob (ScheduledFeeJob feeJob) {
        log.info("Führe monatliche Gebührenbuchung aus: Job {}", feeJob.getId());
        userRepository.findAll().forEach(user -> {
            try {
                accountingService.bookFee(
                        user,
                        feeJob.getAmountInCents(),
                        TransactionType.MONTHLY_CONTRIBUTION,
                        feeJob.getDescription()
                );
            } catch (Exception e) {
                log.error("Fehler bei Gebührenbuchung für User {}: {}", user.getUsername(), e.getMessage());
            }
        });
    }

}