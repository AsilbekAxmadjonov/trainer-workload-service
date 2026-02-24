package com.discovery.workload.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TrainerYearlySummary {
    String trainerUsername;
    String trainerFirstName;
    String trainerLastName;
    String trainerStatus;
    List<Year> years;

    @Value
    @Builder
    public static class Year {
        int year;
        List<Month> months;
    }

    @Value
    @Builder
    public static class Month {
        String month;
        int trainingSummaryDurationMinutes;
    }
}
