package com.discovery.workload.mapper;

import com.discovery.workload.dto.MonthlySummaryResponse;
import com.discovery.workload.model.MonthlySummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MonthlySummaryMapper {
    MonthlySummaryResponse toResponse(MonthlySummary model);
}
