package com.discovery.workload.service;

import com.discovery.workload.dto.MonthlySummaryResponse;
import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.dto.TrainerYearlySummaryResponse;
import org.springframework.http.ResponseEntity;

public interface TrainerWorkloadService {

    ResponseEntity<?> applyEvent(String eventId, TrainerWorkloadRequest request);

    MonthlySummaryResponse getMonthlySummary(String username, int year, int month);

    TrainerYearlySummaryResponse getTrainerSummary(String username);
}
