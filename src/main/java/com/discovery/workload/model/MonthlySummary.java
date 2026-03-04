package com.discovery.workload.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonthlySummary {
    private String username;
    private String firstName;
    private String lastName;
    private boolean active;
    private int year;
    private int month;
    private int totalDurationMinutes;
}
