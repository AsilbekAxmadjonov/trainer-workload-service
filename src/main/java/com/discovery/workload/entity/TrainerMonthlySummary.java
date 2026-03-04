package com.discovery.workload.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trainer_monthly_summary")
public class TrainerMonthlySummary {

    @EmbeddedId
    private TrainerMonthKey id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "total_duration_minutes", nullable = false)
    private int totalDurationMinutes;

    @Version
    private long version;

    public void updateTrainerInfo(String firstName, String lastName, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public void addDuration(int minutes) {
        this.totalDurationMinutes += minutes;
    }

    public void subtractDuration(int minutes) {
        int newTotal = this.totalDurationMinutes - minutes;
        if (newTotal < 0) {
            throw new IllegalStateException("Cannot DELETE: monthly total would become negative.");
        }
        this.totalDurationMinutes = newTotal;
    }

}
