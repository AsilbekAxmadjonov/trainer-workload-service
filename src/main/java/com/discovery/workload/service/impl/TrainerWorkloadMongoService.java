package com.discovery.workload.service.impl;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.model.MonthlySummary;
import com.discovery.workload.model.TrainerYearlySummary;
import com.discovery.workload.mongoDb.document.MonthSummaryDocument;
import com.discovery.workload.mongoDb.document.ProcessedEventDocument;
import com.discovery.workload.mongoDb.document.TrainerSummaryDocument;
import com.discovery.workload.mongoDb.document.YearSummaryDocument;
import com.discovery.workload.mongoDb.repository.ProcessedEventMongoRepository;
import com.discovery.workload.mongoDb.repository.TrainerTrainingSummaryMongoRepository;
import com.discovery.workload.service.TrainerWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

@Service
@Profile("mongo")
@RequiredArgsConstructor
@Slf4j
public class TrainerWorkloadMongoService implements TrainerWorkloadService {

    private final TrainerTrainingSummaryMongoRepository trainerSummaryRepository;
    private final ProcessedEventMongoRepository processedEventRepository;

    @Override
    public void applyEvent(String eventId, TrainerWorkloadRequest request) {

        String safeEventId = normalizeEventId(eventId);

        LocalDate trainingDate = request.getTrainingDate();
        if (trainingDate.isBefore(LocalDate.now())) {
            log.error("Event cannot be in the past. eventId={} trainingDate={}", safeEventId, trainingDate);
            throw new IllegalArgumentException("Event cannot be in the past");
        }

        int summaryYear = trainingDate.getYear();
        int summaryMonth = trainingDate.getMonthValue();
        int durationMinutes = request.getTrainingDurationMinutes();

        log.info("Apply eventId={} trainingId={} username={} actionType={} trainingDate={} durationMinutes={}",
                safeEventId,
                request.getTrainingId(),
                request.getUsername(),
                request.getActionType(),
                trainingDate,
                durationMinutes);

        if (processedEventRepository.existsById(safeEventId)) {
            log.info("Duplicate event ignored. eventId={} trainingId={}", safeEventId, request.getTrainingId());
            return;
        }

        TrainerSummaryDocument trainerDoc = trainerSummaryRepository.findById(request.getUsername())
                .orElseGet(() -> TrainerSummaryDocument.builder()
                        .username(request.getUsername())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .active(Boolean.TRUE.equals(request.getIsActive()))
                        .createdAt(Instant.now())
                        .build());

        trainerDoc.setFirstName(request.getFirstName());
        trainerDoc.setLastName(request.getLastName());
        trainerDoc.setActive(Boolean.TRUE.equals(request.getIsActive()));
        trainerDoc.setUpdatedAt(Instant.now());

        YearSummaryDocument yearDoc = findOrCreateYear(trainerDoc, summaryYear);
        MonthSummaryDocument monthDoc = findOrCreateMonth(yearDoc, summaryMonth);

        long beforeTotal = monthDoc.getTotalDurationMinutes();
        long afterTotal = applyDelta(beforeTotal, durationMinutes, request.getActionType());
        monthDoc.setTotalDurationMinutes(afterTotal);

        log.debug("Updated monthly summary eventId={} username={} year={} month={} before={} delta={} after={}",
                safeEventId, request.getUsername(), summaryYear, summaryMonth, beforeTotal, durationMinutes, afterTotal);

        trainerSummaryRepository.save(trainerDoc);

        processedEventRepository.save(ProcessedEventDocument.builder()
                .eventId(safeEventId)
                .trainingId(request.getTrainingId())
                .processedAt(Instant.now())
                .build());
    }

    @Override
    public MonthlySummary getMonthlySummary(String username, int year, int month) {
        TrainerSummaryDocument trainerDoc = trainerSummaryRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + username));

        long totalMinutes = trainerDoc.getYears().stream()
                .filter(y -> y.getYear() == year)
                .findFirst()
                .flatMap(y -> y.getMonths().stream().filter(m -> m.getMonth() == month).findFirst())
                .map(MonthSummaryDocument::getTotalDurationMinutes)
                .orElse(0L);

        return MonthlySummary.builder()
                .username(trainerDoc.getUsername())
                .firstName(trainerDoc.getFirstName())
                .lastName(trainerDoc.getLastName())
                .active(trainerDoc.isActive())
                .year(year)
                .month(month)
                .totalDurationMinutes(safeLongToInt(totalMinutes))
                .build();
    }

    @Override
    public TrainerYearlySummary getTrainerSummary(String username) {
        TrainerSummaryDocument trainerDoc = trainerSummaryRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + username));

        return TrainerYearlySummary.builder()
                .trainerUsername(trainerDoc.getUsername())
                .trainerFirstName(trainerDoc.getFirstName())
                .trainerLastName(trainerDoc.getLastName())
                .trainerStatus(trainerDoc.isActive() ? "ACTIVE" : "INACTIVE")
                .years(trainerDoc.getYears().stream()
                        .sorted(Comparator.comparingInt(YearSummaryDocument::getYear))
                        .map(y -> TrainerYearlySummary.Year.builder()
                                .year(y.getYear())
                                .months(y.getMonths().stream()
                                        .sorted(Comparator.comparingInt(MonthSummaryDocument::getMonth))
                                        .map(m -> TrainerYearlySummary.Month.builder()
                                                .month(Month.of(m.getMonth())
                                                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                                                .trainingSummaryDurationMinutes(safeLongToInt(m.getTotalDurationMinutes()))
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    private String normalizeEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            String generated = UUID.randomUUID().toString();
            log.warn("Missing X-Event-Id header. Generated new eventId={}", generated);
            return generated;
        }
        return eventId.trim();
    }

    private static long applyDelta(long currentTotal, int durationMinutes, ActionType actionType) {
        if (actionType == ActionType.ADD) {
            return currentTotal + durationMinutes;
        }

        long newTotal = currentTotal - durationMinutes;
        if (newTotal < 0) {
            throw new IllegalStateException("Cannot DELETE: monthly total would become negative.");
        }
        return newTotal;
    }

    private static YearSummaryDocument findOrCreateYear(TrainerSummaryDocument trainerDoc, int year) {
        return trainerDoc.getYears().stream()
                .filter(y -> y.getYear() == year)
                .findFirst()
                .orElseGet(() -> {
                    YearSummaryDocument created = YearSummaryDocument.builder()
                            .year(year)
                            .build();
                    trainerDoc.getYears().add(created);
                    return created;
                });
    }

    private static MonthSummaryDocument findOrCreateMonth(YearSummaryDocument yearDoc, int month) {
        return yearDoc.getMonths().stream()
                .filter(m -> m.getMonth() == month)
                .findFirst()
                .orElseGet(() -> {
                    MonthSummaryDocument created = MonthSummaryDocument.builder()
                            .month(month)
                            .totalDurationMinutes(0L)
                            .build();
                    yearDoc.getMonths().add(created);
                    return created;
                });
    }

    private static int safeLongToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }
}