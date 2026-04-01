package com.discovery.workload.mongoDb.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "processed_event")
public class ProcessedEventDocument {

    @Id
    private String eventId;

    private String trainingId;
    private Instant processedAt;
}
