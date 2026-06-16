package de.tozwhv.vereinskasse.server.modell.systemjob;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "job_type", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter
public abstract class SystemJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    private boolean enabled;

    public abstract String getJobType();
}