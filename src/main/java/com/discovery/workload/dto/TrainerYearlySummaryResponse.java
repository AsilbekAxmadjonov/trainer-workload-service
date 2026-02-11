package com.discovery.workload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerYearlySummaryResponse {

    private String trainerUsername;
    private String trainerFirstName;
    private String trainerLastName;
    private String trainerStatus;

    private List<YearDto> years;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearDto {
        private int year;
        private List<MonthDto> months;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthDto {
        private String month; // "Jan", "Feb", ...
        private int trainingSummaryDurationMinutes; // minutes
    }
}
