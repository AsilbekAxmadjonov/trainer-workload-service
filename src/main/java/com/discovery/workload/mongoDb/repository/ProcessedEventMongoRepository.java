package com.discovery.workload.mongoDb.repository;

import com.discovery.workload.mongoDb.document.ProcessedEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedEventMongoRepository
        extends MongoRepository<ProcessedEventDocument, String> {
}