package com.discovery.workload.mongoDb.document;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearSummaryDocument {

    private int year;

    @Builder.Default
    private List<MonthSummaryDocument> months = new ArrayList<>();
}
