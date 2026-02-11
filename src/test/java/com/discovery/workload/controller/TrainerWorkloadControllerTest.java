package com.discovery.workload.controller;

import com.discovery.workload.dto.MonthlySummaryResponse;
import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.dto.TrainerYearlySummaryResponse;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.service.TrainerWorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrainerWorkloadControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private TrainerWorkloadService trainerWorkloadService;

    @BeforeEach
    void setUp() {
        trainerWorkloadService = Mockito.mock(TrainerWorkloadService.class);
        TrainerWorkloadController controller = new TrainerWorkloadController(trainerWorkloadService);

        objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testEndpoint_returnsOkString() throws Exception {
        mockMvc.perform(get("/api/v1/workloads/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("workload-service OK"));
    }

    @Test
    void applyEvent_withHeader_returnsServiceResponse() throws Exception {
        TrainerWorkloadRequest req = TrainerWorkloadRequest.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingId("t-1")
                .trainingDate(LocalDate.now().plusDays(1))
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();

        doReturn(ResponseEntity.ok("Saved"))
                .when(trainerWorkloadService)
                .applyEvent(eq("evt-1"), any(TrainerWorkloadRequest.class));

        mockMvc.perform(post("/api/v1/workloads/events")
                        .header("X-Event-Id", "evt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Saved"));

        verify(trainerWorkloadService).applyEvent(eq("evt-1"), any(TrainerWorkloadRequest.class));
    }

    @Test
    void applyEvent_withoutHeader_callsServiceWithNull() throws Exception {
        TrainerWorkloadRequest req = TrainerWorkloadRequest.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingId("t-1")
                .trainingDate(LocalDate.now().plusDays(1))
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();

        doReturn(ResponseEntity.ok("Saved"))
                .when(trainerWorkloadService)
                .applyEvent(isNull(), any(TrainerWorkloadRequest.class));

        mockMvc.perform(post("/api/v1/workloads/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Saved"));

        verify(trainerWorkloadService).applyEvent(isNull(), any(TrainerWorkloadRequest.class));
    }

    @Test
    void getMonthlySummary_returnsJsonFromService() throws Exception {
        MonthlySummaryResponse resp = MonthlySummaryResponse.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .year(2026)
                .month(2)
                .totalDurationMinutes(120)
                .build();

        when(trainerWorkloadService.getMonthlySummary("john", 2026, 2)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/workloads/john/2026/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(2))
                .andExpect(jsonPath("$.totalDurationMinutes").value(120));
    }

    @Test
    void getTrainerSummary_returnsJsonFromService() throws Exception {
        TrainerYearlySummaryResponse resp = TrainerYearlySummaryResponse.builder()
                .trainerUsername("john")
                .trainerStatus("ACTIVE")
                .years(List.of(
                        TrainerYearlySummaryResponse.YearDto.builder()
                                .year(2026)
                                .months(List.of(
                                        TrainerYearlySummaryResponse.MonthDto.builder()
                                                .month("Jan")
                                                .trainingSummaryDurationMinutes(100)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(trainerWorkloadService.getTrainerSummary("john")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/workloads/trainers/john/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.trainerUsername").value("john"))
                .andExpect(jsonPath("$.trainerStatus").value("ACTIVE"));
    }
}
