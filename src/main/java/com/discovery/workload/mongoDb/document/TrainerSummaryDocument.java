package com.discovery.workload.mongoDb.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "trainer_training_summary")
@CompoundIndex(name = "idx_first_last", def = "{'firstName': 1, 'lastName': 1}")
public class TrainerSummaryDocument {

    @Id
    private String username;

    private String firstName;
    private String lastName;
    private boolean active;

    @Builder.Default
    private List<YearSummaryDocument> years = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
