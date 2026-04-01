package com.discovery.workload.cucumber.integration;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.messaging.WorkloadEventListener;
import com.discovery.workload.messaging.WorkloadEventMessage;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.model.MonthlySummary;
import com.discovery.workload.mongoDb.repository.ProcessedEventMongoRepository;
import com.discovery.workload.mongoDb.repository.TrainerTrainingSummaryMongoRepository;
import com.discovery.workload.service.TrainerWorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.*;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;


import java.time.LocalDate;
import java.util.UUID;


@RequiredArgsConstructor
public class WorkloadEventListenerSteps {

    private final WorkloadEventListener listener;
    private final TrainerWorkloadService trainerWorkloadService;
    private final TrainerTrainingSummaryMongoRepository trainerSummaryRepository;
    private final ProcessedEventMongoRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    private String payload;
    private Exception thrownException;
    private String lastEventId;
    private final LocalDate futureDate = LocalDate.now().plusDays(1);

    @Given("trainer workload repositories are clean")
    public void trainer_workload_repositories_are_clean() {
        trainerSummaryRepository.deleteAll();
        processedEventRepository.deleteAll();
        payload = null;
        thrownException = null;
        lastEventId = null;
    }

    @Given("a workload ADD event for trainer {string} with duration {int}")
    public void a_workload_add_event_for_trainer_with_duration(String username, int duration) throws Exception {
        lastEventId = UUID.randomUUID().toString();
        payload = buildPayload(lastEventId, username, duration, ActionType.ADD);
    }

    @Given("an existing monthly summary created by ADD event for trainer {string} with duration {int}")
    public void an_existing_monthly_summary_created_by_add_event(String username, int duration) throws Exception {
        String eventId = UUID.randomUUID().toString();
        String initialPayload = buildPayload(eventId, username, duration, ActionType.ADD);
        listener.onMessage(initialPayload);
    }

    @Given("a workload DELETE event for trainer {string} with duration {int}")
    public void a_workload_delete_event_for_trainer_with_duration(String username, int duration) throws Exception {
        lastEventId = UUID.randomUUID().toString();
        payload = buildPayload(lastEventId, username, duration, ActionType.DELETE);
    }

    @Given("a workload ADD event with event id {string} for trainer {string} with duration {int}")
    public void a_workload_add_event_with_event_id_for_trainer_with_duration(String eventId, String username, int duration) throws Exception {
        lastEventId = eventId;
        payload = buildPayload(lastEventId, username, duration, ActionType.ADD);
    }

    @Given("a workload event without event id for trainer {string} with duration {int}")
    public void a_workload_event_without_event_id_for_trainer_with_duration(String username, int duration) throws Exception {
        WorkloadEventMessage message = WorkloadEventMessage.builder()
                .eventId(null)
                .transactionId("tx-" + UUID.randomUUID())
                .request(buildRequest(username, duration, ActionType.ADD))
                .build();

        payload = objectMapper.writeValueAsString(message);
    }

    @When("the workload listener consumes the event")
    public void the_workload_listener_consumes_the_event() {
        thrownException = null;
        try {
            listener.onMessage(payload);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the same workload listener event is consumed again")
    public void the_same_workload_listener_event_is_consumed_again() {
        try {
            listener.onMessage(payload);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the workload listener consumes the invalid event")
    public void the_workload_listener_consumes_the_invalid_event() {
        try {
            listener.onMessage(payload);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @Then("monthly summary for trainer {string} should have total duration {int}")
    public void monthly_summary_for_trainer_should_have_total_duration(String username, int expectedDuration) {
        MonthlySummary summary = trainerWorkloadService.getMonthlySummary(
                username,
                futureDate.getYear(),
                futureDate.getMonthValue()
        );

        Assertions.assertEquals(expectedDuration, summary.getTotalDurationMinutes());
    }

    @Then("listener processing should fail")
    public void listener_processing_should_fail() {
        Assertions.assertNotNull(thrownException);
        Assertions.assertInstanceOf(RuntimeException.class, thrownException);
    }

    private String buildPayload(String eventId, String username, int duration, ActionType actionType) throws Exception {
        WorkloadEventMessage message = WorkloadEventMessage.builder()
                .eventId(eventId)
                .transactionId("tx-" + UUID.randomUUID())
                .request(buildRequest(username, duration, actionType))
                .build();

        return objectMapper.writeValueAsString(message);
    }

    private TrainerWorkloadRequest buildRequest(String username, int duration, ActionType actionType) {
        return TrainerWorkloadRequest.builder()
                .trainingId("training-" + UUID.randomUUID())
                .username(username)
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingDate(futureDate)
                .trainingDurationMinutes(duration)
                .actionType(actionType)
                .build();
    }
}
