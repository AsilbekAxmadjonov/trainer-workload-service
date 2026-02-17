package com.discovery.workload.mapper;

import com.discovery.workload.dto.TrainerYearlySummaryResponse;
import com.discovery.workload.model.TrainerYearlySummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainerYearlySummaryMapper {
    TrainerYearlySummaryResponse toResponse(TrainerYearlySummary model);
}

