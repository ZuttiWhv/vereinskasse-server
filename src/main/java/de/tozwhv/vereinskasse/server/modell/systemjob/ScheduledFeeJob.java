package de.tozwhv.vereinskasse.server.modell.systemjob;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("SCHEDULED_FEE")
@Getter @Setter
public class ScheduledFeeJob extends SystemJob {

    private Integer amountInCents;
    private String description;

    @Override
    public String getJobType() {
        return "SCHEDULED_FEE";
    }
}