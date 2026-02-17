package com.discovery.workload.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonthlySummary {
    String username;
    String firstName;
    String lastName;
    boolean active;
    int year;
    int month;
    int totalDurationMinutes;
}
