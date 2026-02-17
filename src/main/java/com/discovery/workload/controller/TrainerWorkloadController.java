package com.discovery.workload.controller;

import com.discovery.workload.dto.MonthlySummaryResponse;
import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.dto.TrainerYearlySummaryResponse;
import com.discovery.workload.mapper.MonthlySummaryMapper;
import com.discovery.workload.mapper.TrainerYearlySummaryMapper;
import com.discovery.workload.model.MonthlySummary;
import com.discovery.workload.model.TrainerYearlySummary;
import com.discovery.workload.service.TrainerWorkloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workloads")
@RequiredArgsConstructor
@Validated
public class TrainerWorkloadController {

    private final TrainerWorkloadService trainerWorkloadService;
    private final MonthlySummaryMapper monthlySummaryMapper;
    private final TrainerYearlySummaryMapper trainerYearlySummaryMapper;

    @PostMapping("/events")
    public ResponseEntity<Void> applyEvent(
            @RequestHeader(value = "X-Event-Id", required = false) String eventId,
            @Valid @RequestBody TrainerWorkloadRequest request
    ) {
        trainerWorkloadService.applyEvent(eventId, request);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{username}/{year}/{month}")
    public ResponseEntity<MonthlySummaryResponse> getMonthly(
            @PathVariable String username,
            @PathVariable int year,
            @PathVariable int month
    ) {
        MonthlySummary model = trainerWorkloadService.getMonthlySummary(username, year, month);
        MonthlySummaryResponse response = monthlySummaryMapper.toResponse(model);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trainers/{username}/summary")
    public ResponseEntity<TrainerYearlySummaryResponse> getYearly(
            @PathVariable String username
    ) {
        TrainerYearlySummary model = trainerWorkloadService.getTrainerSummary(username);
        TrainerYearlySummaryResponse response = trainerYearlySummaryMapper.toResponse(model);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/test")
    public String test() {
        return "workload-service OK";
    }
}




