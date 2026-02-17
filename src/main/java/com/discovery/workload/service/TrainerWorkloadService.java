package com.discovery.workload.service;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.model.MonthlySummary;
import com.discovery.workload.model.TrainerYearlySummary;

public interface TrainerWorkloadService {

    void applyEvent(String eventId, TrainerWorkloadRequest request);

    MonthlySummary getMonthlySummary(String username, int year, int month);

    TrainerYearlySummary getTrainerSummary(String username);
}
