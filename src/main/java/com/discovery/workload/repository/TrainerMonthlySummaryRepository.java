package com.discovery.workload.repository;

import com.discovery.workload.entity.TrainerMonthKey;
import com.discovery.workload.entity.TrainerMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainerMonthlySummaryRepository extends JpaRepository<TrainerMonthlySummary, TrainerMonthKey> {
    List<TrainerMonthlySummary> findAllByIdUsernameOrderByIdYearAscIdMonthAsc(String username);
}
