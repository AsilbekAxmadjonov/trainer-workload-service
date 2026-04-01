package com.discovery.workload.mongoDb.repository;

import com.discovery.workload.mongoDb.document.TrainerSummaryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainerTrainingSummaryMongoRepository
        extends MongoRepository<TrainerSummaryDocument, String> {
}