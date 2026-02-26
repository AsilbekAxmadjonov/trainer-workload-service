package com.discovery.workload.messaging;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkloadEventMessage {
    private String eventId;
    private String transactionId;
    private TrainerWorkloadRequest request;
}
