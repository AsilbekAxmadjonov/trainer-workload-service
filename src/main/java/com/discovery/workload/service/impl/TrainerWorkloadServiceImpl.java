package com.discovery.workload.service.impl;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.entity.ProcessedEventEntity;
import com.discovery.workload.entity.TrainerMonthKey;
import com.discovery.workload.entity.TrainerMonthlySummary;
import com.discovery.workload.exception.NotFoundException;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.model.MonthlySummary;
import com.discovery.workload.model.TrainerYearlySummary;
import com.discovery.workload.repository.ProcessedEventRepository;
import com.discovery.workload.repository.TrainerMonthlySummaryRepository;
import com.discovery.workload.service.TrainerWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {

    private final TrainerMonthlySummaryRepository repository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void applyEvent(String eventId, TrainerWorkloadRequest request) {

        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
            log.warn("Missing X-Event-Id header. Generated new eventId={}", eventId);
        }

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event ignored. eventId={}", eventId);
            return;
        }

        LocalDate date = request.getTrainingDate();
        if (date.isBefore(LocalDate.now())) {
            log.error("Event cannot be in the past");
            throw new IllegalArgumentException("Event cannot be in the past");
        }

        int year = date.getYear();
        int month = date.getMonthValue();

        TrainerMonthKey key = new TrainerMonthKey(request.getUsername(), year, month);

        TrainerMonthlySummary summary = repository.findById(key)
                .orElseGet(() -> TrainerMonthlySummary.builder()
                        .id(key)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .active(Boolean.TRUE.equals(request.getIsActive()))
                        .totalDurationMinutes(0)
                        .build());

        summary.updateTrainerInfo(
                request.getFirstName(),
                request.getLastName(),
                Boolean.TRUE.equals(request.getIsActive())
        );

        int duration = request.getTrainingDurationMinutes();
        if (request.getActionType() == ActionType.ADD) summary.addDuration(duration);
        else summary.subtractDuration(duration);

        repository.save(summary);
        processedEventRepository.save(new ProcessedEventEntity(eventId, request.getTrainingId()));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummary getMonthlySummary(String username, int year, int month) {

        TrainerMonthKey key = new TrainerMonthKey(username, year, month);

        TrainerMonthlySummary summary = repository.findById(key)
                .orElseThrow(() -> new NotFoundException(
                        "No summary found for username=%s year=%d month=%d".formatted(username, year, month)
                ));

        return MonthlySummary.builder()
                .username(summary.getId().getUsername())
                .firstName(summary.getFirstName())
                .lastName(summary.getLastName())
                .active(summary.isActive())
                .year(summary.getId().getYear())
                .month(summary.getId().getMonth())
                .totalDurationMinutes(summary.getTotalDurationMinutes())
                .build();
    }



    @Override
    @Transactional(readOnly = true)
    public TrainerYearlySummary getTrainerSummary(String username) {

        List<TrainerMonthlySummary> rows =
                repository.findAllByIdUsernameOrderByIdYearAscIdMonthAsc(username);

        if (rows.isEmpty()) {
            throw new NotFoundException("No summary found for username=" + username);
        }

        TrainerMonthlySummary any = rows.get(rows.size() - 1);
        String status = any.isActive() ? "ACTIVE" : "INACTIVE";

        Map<Integer, List<TrainerMonthlySummary>> byYear =
                rows.stream().collect(Collectors.groupingBy(
                        r -> r.getId().getYear(), LinkedHashMap::new, Collectors.toList()
                ));

        List<TrainerYearlySummary.Year> years = new ArrayList<>();

        for (Map.Entry<Integer, List<TrainerMonthlySummary>> entry : byYear.entrySet()) {
            int year = entry.getKey();

            List<TrainerYearlySummary.Month> months = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(r -> r.getId().getMonth()))
                    .map(r -> TrainerYearlySummary.Month.builder()
                            .month(shortMonth(r.getId().getMonth()))
                            .trainingSummaryDurationMinutes(r.getTotalDurationMinutes())
                            .build())
                    .toList();

            years.add(TrainerYearlySummary.Year.builder()
                    .year(year)
                    .months(months)
                    .build());
        }

        return TrainerYearlySummary.builder()
                .trainerUsername(username)
                .trainerFirstName(any.getFirstName())
                .trainerLastName(any.getLastName())
                .trainerStatus(status)
                .years(years)
                .build();
    }


    private String shortMonth(int monthNumber1to12) {
        return Month.of(monthNumber1to12).getDisplayName(TextStyle.SHORT, ENGLISH); // Jan, Feb, Mar...
    }
}
