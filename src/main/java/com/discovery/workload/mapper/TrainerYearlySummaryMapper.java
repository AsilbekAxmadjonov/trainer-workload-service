package com.discovery.workload.mapper;

import com.discovery.workload.dto.TrainerYearlySummaryResponse;
import com.discovery.workload.model.TrainerYearlySummary;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TrainerYearlySummaryMapper {
    TrainerYearlySummaryResponse toResponse(TrainerYearlySummary model);
}
