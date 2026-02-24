package com.discovery.workload.mapper;

import com.discovery.workload.dto.MonthlySummaryResponse;
import com.discovery.workload.model.MonthlySummary;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MonthlySummaryMapper {
    MonthlySummaryResponse toResponse(MonthlySummary model);
}