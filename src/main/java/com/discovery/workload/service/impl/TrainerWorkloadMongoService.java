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
import java.util.List;
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
        LocalDate trainingDate = validateTrainingDate(request.getTrainingDate(), safeEventId);

        logApplyEvent(safeEventId, request, trainingDate);

        if (isDuplicateEvent(safeEventId, request.getTrainingId())) {
            return;
        }

        int summaryYear = trainingDate.getYear();
        int summaryMonth = trainingDate.getMonthValue();
        int durationMinutes = request.getTrainingDurationMinutes();

        TrainerSummaryDocument trainerDoc = getOrCreateTrainerSummary(request);
        updateTrainerInfo(trainerDoc, request);
        updateMonthlySummary(trainerDoc, summaryYear, summaryMonth, durationMinutes, request.getActionType(), safeEventId);

        trainerSummaryRepository.save(trainerDoc);
        saveProcessedEvent(safeEventId, request.getTrainingId());
    }

    @Override
    public MonthlySummary getMonthlySummary(String username, int year, int month) {
        TrainerSummaryDocument trainerDoc = getTrainerByUsername(username);
        long totalMinutes = extractMonthlyTotal(trainerDoc, year, month);

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
        TrainerSummaryDocument trainerDoc = getTrainerByUsername(username);

        return TrainerYearlySummary.builder()
                .trainerUsername(trainerDoc.getUsername())
                .trainerFirstName(trainerDoc.getFirstName())
                .trainerLastName(trainerDoc.getLastName())
                .trainerStatus(resolveTrainerStatus(trainerDoc))
                .years(buildYearlySummary(trainerDoc))
                .build();
    }

    private String normalizeEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            String generatedEventId = UUID.randomUUID().toString();
            log.warn("Missing X-Event-Id header. Generated new eventId={}", generatedEventId);
            return generatedEventId;
        }
        return eventId.trim();
    }

    private LocalDate validateTrainingDate(LocalDate trainingDate, String eventId) {
        if (trainingDate.isBefore(LocalDate.now())) {
            log.error("Event cannot be in the past. eventId={} trainingDate={}", eventId, trainingDate);
            throw new IllegalArgumentException("Event cannot be in the past");
        }
        return trainingDate;
    }

    private void logApplyEvent(String eventId, TrainerWorkloadRequest request, LocalDate trainingDate) {
        log.info("Apply eventId={} trainingId={} username={} actionType={} trainingDate={} durationMinutes={}",
                eventId,
                request.getTrainingId(),
                request.getUsername(),
                request.getActionType(),
                trainingDate,
                request.getTrainingDurationMinutes());
    }

    private boolean isDuplicateEvent(String eventId, String trainingId) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event ignored. eventId={} trainingId={}", eventId, trainingId);
            return true;
        }
        return false;
    }

    private TrainerSummaryDocument getOrCreateTrainerSummary(TrainerWorkloadRequest request) {
        return trainerSummaryRepository.findById(request.getUsername())
                .orElseGet(() -> TrainerSummaryDocument.builder()
                        .username(request.getUsername())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .active(Boolean.TRUE.equals(request.getIsActive()))
                        .build());
    }

    private void updateTrainerInfo(TrainerSummaryDocument trainerDoc, TrainerWorkloadRequest request) {
        trainerDoc.setFirstName(request.getFirstName());
        trainerDoc.setLastName(request.getLastName());
        trainerDoc.setActive(Boolean.TRUE.equals(request.getIsActive()));
    }

    private void updateMonthlySummary(TrainerSummaryDocument trainerDoc,
                                      int summaryYear,
                                      int summaryMonth,
                                      int durationMinutes,
                                      ActionType actionType,
                                      String eventId) {

        YearSummaryDocument yearDoc = findOrCreateYear(trainerDoc, summaryYear);
        MonthSummaryDocument monthDoc = findOrCreateMonth(yearDoc, summaryMonth);

        long beforeTotal = monthDoc.getTotalDurationMinutes();
        long afterTotal = applyDelta(beforeTotal, durationMinutes, actionType);
        monthDoc.setTotalDurationMinutes(afterTotal);

        log.debug("Updated monthly summary eventId={} username={} year={} month={} before={} delta={} after={}",
                eventId, trainerDoc.getUsername(), summaryYear, summaryMonth, beforeTotal, durationMinutes, afterTotal);
    }

    private void saveProcessedEvent(String eventId, String trainingId) {
        processedEventRepository.save(ProcessedEventDocument.builder()
                .eventId(eventId)
                .trainingId(trainingId)
                .processedAt(Instant.now())
                .build());
    }

    private TrainerSummaryDocument getTrainerByUsername(String username) {
        return trainerSummaryRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + username));
    }

    private long extractMonthlyTotal(TrainerSummaryDocument trainerDoc, int year, int month) {
        return trainerDoc.getYears().stream()
                .filter(y -> y.getYear() == year)
                .findFirst()
                .flatMap(y -> y.getMonths().stream()
                        .filter(m -> m.getMonth() == month)
                        .findFirst())
                .map(MonthSummaryDocument::getTotalDurationMinutes)
                .orElse(0L);
    }

    private String resolveTrainerStatus(TrainerSummaryDocument trainerDoc) {
        return trainerDoc.isActive() ? "ACTIVE" : "INACTIVE";
    }

    private List<TrainerYearlySummary.Year> buildYearlySummary(TrainerSummaryDocument trainerDoc) {
        return trainerDoc.getYears().stream()
                .sorted(Comparator.comparingInt(YearSummaryDocument::getYear))
                .map(this::mapYearSummary)
                .toList();
    }

    private TrainerYearlySummary.Year mapYearSummary(YearSummaryDocument yearDoc) {
        return TrainerYearlySummary.Year.builder()
                .year(yearDoc.getYear())
                .months(buildMonthSummaries(yearDoc))
                .build();
    }

    private List<TrainerYearlySummary.Month> buildMonthSummaries(YearSummaryDocument yearDoc) {
        return yearDoc.getMonths().stream()
                .sorted(Comparator.comparingInt(MonthSummaryDocument::getMonth))
                .map(this::mapMonthSummary)
                .toList();
    }

    private TrainerYearlySummary.Month mapMonthSummary(MonthSummaryDocument monthDoc) {
        return TrainerYearlySummary.Month.builder()
                .month(toShortMonthName(monthDoc.getMonth()))
                .trainingSummaryDurationMinutes(safeLongToInt(monthDoc.getTotalDurationMinutes()))
                .build();
    }

    private String toShortMonthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
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