package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.jobs.ScheduledFeeJobDTO;
import de.tozwhv.vereinskasse.server.modell.systemjob.ScheduledFeeJob;
import de.tozwhv.vereinskasse.server.modell.systemjob.SystemJob;
import de.tozwhv.vereinskasse.server.repository.JobRepository;
import de.tozwhv.vereinskasse.server.service.JobSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final JobSchedulerService jobSchedulerService;

    /**
     * Listet alle Jobs auf (nur für berechtigte Nutzer).
     */
    @PreAuthorize("hasAuthority('READ_JOBS')")
    @GetMapping
    public List<SystemJob> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * Erstellt einen NEUEN Gebühren-Job.
     * Akzeptiert nur das spezifische DTO, um Mass-Assignment zu verhindern.
     */
    @PreAuthorize("hasAuthority('WRITE_JOBS')")
    @PostMapping("/fee")
    public ResponseEntity<SystemJob> createFeeJob(@RequestBody ScheduledFeeJobDTO dto) {
        ScheduledFeeJob newJob = new ScheduledFeeJob();
        newJob.setCronExpression(dto.cronExpression());
        newJob.setEnabled(dto.enabled());
        newJob.setAmountInCents(dto.amountInCents());
        newJob.setDescription(dto.description());

        SystemJob savedJob = jobRepository.save(newJob);
        jobSchedulerService.refreshAllJobs();
        return ResponseEntity.ok(savedJob);
    }

    /**
     * Aktualisiert einen bestehenden Gebühren-Job.
     * Verhindert das Ändern des Job-Typs (STI-Schutz).
     */
    @PreAuthorize("hasAuthority('WRITE_JOBS')")
    @PutMapping("/fee/{id}")
    public ResponseEntity<SystemJob> updateFeeJob(@PathVariable Long id, @RequestBody ScheduledFeeJobDTO dto) {
        return jobRepository.findById(id).map(existingJob -> {
            if (!(existingJob instanceof ScheduledFeeJob feeJob)) {
                return ResponseEntity.badRequest().<SystemJob>build();
            }

            feeJob.setCronExpression(dto.cronExpression());
            feeJob.setEnabled(dto.enabled());
            feeJob.setAmountInCents(dto.amountInCents());
            feeJob.setDescription(dto.description());

            SystemJob savedJob = jobRepository.save(feeJob);
            jobSchedulerService.refreshAllJobs();
            return ResponseEntity.ok(savedJob);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Löscht einen Gebühren-Job.
     * Prüft vorher, ob die ID wirklich zu einem FeeJob gehört, damit ein Kassenwart
     * nicht die ID eines Backup-Jobs löschen kann.
     */
    @PreAuthorize("hasAuthority('DELETE_JOBS')")
    @DeleteMapping("/fee/{id}")
    public ResponseEntity<Void> deleteFeeJob(@PathVariable Long id) {
        return jobRepository.findById(id).map(existingJob -> {
            if (!(existingJob instanceof ScheduledFeeJob)) {
                return ResponseEntity.badRequest().<Void>build();
            }

            jobRepository.deleteById(id);
            jobSchedulerService.refreshAllJobs();
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('TRIGGER_JOBS')")
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerJobManually(@PathVariable Long id) {
        jobRepository.findById(id).ifPresent(jobSchedulerService::executeJobDirectly);
        return ResponseEntity.accepted().build();
    }
}