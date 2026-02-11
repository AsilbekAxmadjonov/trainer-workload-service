package com.discovery.workload.controller;

import com.discovery.workload.dto.MonthlySummaryResponse;
import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.dto.TrainerYearlySummaryResponse;
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

    @PostMapping("/events")
    public ResponseEntity<?> applyEvent(
            @RequestHeader(name = "X-Event-Id", required = false) String eventId,
            @Valid @RequestBody TrainerWorkloadRequest request
    ) {
        return trainerWorkloadService.applyEvent(eventId, request);
    }


    @GetMapping("/{username}/{year}/{month}")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @PathVariable String username,
            @PathVariable int year,
            @PathVariable int month
    ) {
        MonthlySummaryResponse res = trainerWorkloadService.getMonthlySummary(username, year, month);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/trainers/{username}/summary")
    public TrainerYearlySummaryResponse getTrainerSummary(@PathVariable String username) {
        return trainerWorkloadService.getTrainerSummary(username);
    }



    @GetMapping("/test")
    public String test() {
        return "workload-service OK";
    }
}




