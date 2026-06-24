package de.tozwhv.vereinskasse.server.controller;

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

    @PreAuthorize("hasAuthority('READ_JOBS')")
    @GetMapping
    public List<SystemJob> getAllJobs() {
        return jobRepository.findAll();
    }

    @PreAuthorize("hasAuthority('WRITE_JOBS')")
    @PostMapping
    public ResponseEntity<SystemJob> createJob(@RequestBody SystemJob job) {
        SystemJob savedJob = jobRepository.save(job);
        jobSchedulerService.refreshAllJobs(); // Scheduler neu laden
        return ResponseEntity.ok(savedJob);
    }

    @PreAuthorize("hasAuthority('DELETE_JOBS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobRepository.deleteById(id);
        jobSchedulerService.refreshAllJobs(); // Scheduler neu laden
        return ResponseEntity.noContent().build();
    }

    // Praktisch für den Admin: Manuelle Auslösung
    @PreAuthorize("hasAuthority('TRIGGER_JOBS')")
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerJobManually(@PathVariable Long id) {
        jobRepository.findById(id).ifPresent(jobSchedulerService::executeJobDirectly);
        return ResponseEntity.accepted().build();
    }
}