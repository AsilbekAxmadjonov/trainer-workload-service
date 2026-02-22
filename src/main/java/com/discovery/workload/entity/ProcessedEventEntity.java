package com.discovery.workload.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(nullable = false)
    private String trainingId;

    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedEventEntity(String eventId, String trainingId) {
        this.eventId = eventId;
        this.trainingId = trainingId;
        this.processedAt = Instant.now();
    }
}
