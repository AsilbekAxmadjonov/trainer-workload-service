package com.discovery.workload.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryResponse {
    private String username;
    private String firstName;
    private String lastName;
    private boolean active;
    private int year;
    private int month;
    private int totalDurationMinutes;
}
