package com.discovery.workload.mongoDb.document;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSummaryDocument {

    private int month;
    private long totalDurationMinutes;
}
