package com.discovery.workload.messaging;

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

    @JmsListener(destination = Queues.WORKLOAD_EVENTS)
    public void onMessage(WorkloadEventMessage message) {

        if (message == null || message.getRequest() == null) {
            log.error("Invalid message: null body");
            return;
        }

        trainerWorkloadService.applyEvent(message.getEventId(), message.getRequest());
    }
}
