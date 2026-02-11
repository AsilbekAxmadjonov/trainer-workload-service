package com.discovery.workload.repository;

import com.discovery.workload.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
    boolean existsByTrainingId(String trainingId);
}
