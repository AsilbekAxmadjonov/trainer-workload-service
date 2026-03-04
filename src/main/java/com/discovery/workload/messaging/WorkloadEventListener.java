package com.discovery.workload.messaging;

import com.discovery.workload.filter.TransactionIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.discovery.workload.service.TrainerWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadEventListener {

    private final TrainerWorkloadService trainerWorkloadService;
    private final ObjectMapper objectMapper;

    @JmsListener(destination = Queues.WORKLOAD_EVENTS)
    public void onMessage(String json) {

        WorkloadEventMessage message = null;

        try {
            message = objectMapper.readValue(json, WorkloadEventMessage.class);

            if (message == null || message.getRequest() == null) {
                throw new IllegalArgumentException("Invalid message payload");
            }

            if (message.getEventId() == null || message.getEventId().isBlank()) {
                throw new IllegalArgumentException("Missing eventId");
            }

            if (message.getTransactionId() != null) {
                MDC.put(TransactionIdFilter.MDC_KEY, message.getTransactionId());
            }

            log.info(
                    "Consumed JMS eventId={} trainingId={} txId={}",
                    message.getEventId(),
                    message.getRequest().getTrainingId(),
                    message.getTransactionId()
            );

            trainerWorkloadService.applyEvent(
                    message.getEventId(),
                    message.getRequest()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process JMS message eventId={}",
                    message != null ? message.getEventId() : "unknown",
                    e
            );
            throw new RuntimeException("Failed to process workload event", e);

        } finally {
            MDC.clear();
        }
    }
}