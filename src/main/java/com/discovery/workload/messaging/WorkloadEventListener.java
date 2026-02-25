package com.discovery.workload.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.discovery.workload.service.TrainerWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        try {
            WorkloadEventMessage message = objectMapper.readValue(json, WorkloadEventMessage.class);

            if (message == null || message.getRequest() == null) {
                log.error("Invalid message: {}", json);
                return;
            }

            log.info("Consumed JMS eventId={} trainingId={}",
                    message.getEventId(),
                    message.getRequest().getTrainingId());

            trainerWorkloadService.applyEvent(message.getEventId(), message.getRequest());

        } catch (Exception e) {
            log.error("Failed to parse message: {}", json, e);
        }
    }
}