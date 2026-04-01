package com.discovery.workload.service.impl;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.mongoDb.document.TrainerSummaryDocument;
import com.discovery.workload.mongoDb.repository.ProcessedEventMongoRepository;
import com.discovery.workload.mongoDb.repository.TrainerTrainingSummaryMongoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadMongoServiceTest {

    @Mock
    private TrainerTrainingSummaryMongoRepository trainerSummaryRepository;

    @Mock
    private ProcessedEventMongoRepository processedEventRepository;

    @InjectMocks
    private TrainerWorkloadMongoService service;

    @Test
    void applyEvent_shouldCreateTrainerAndSaveEvent() {

        TrainerWorkloadRequest request = new TrainerWorkloadRequest();
        request.setUsername("trainer1");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setIsActive(true);
        request.setTrainingId("training1");
        request.setTrainingDurationMinutes(60);
        request.setTrainingDate(LocalDate.now().plusDays(1));
        request.setActionType(ActionType.ADD);

        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(trainerSummaryRepository.findById("trainer1"))
                .thenReturn(Optional.empty());

        service.applyEvent("event1", request);

        verify(trainerSummaryRepository, times(1)).save(any(TrainerSummaryDocument.class));
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    void applyEvent_shouldIgnoreDuplicateEvent() {

        TrainerWorkloadRequest request = new TrainerWorkloadRequest();
        request.setUsername("trainer1");
        request.setTrainingId("training1");
        request.setTrainingDurationMinutes(60);
        request.setTrainingDate(LocalDate.now().plusDays(1));
        request.setActionType(ActionType.ADD);

        when(processedEventRepository.existsById("event1")).thenReturn(true);

        service.applyEvent("event1", request);

        verify(trainerSummaryRepository, never()).save(any());
    }

    @Test
    void applyEvent_shouldThrowException_whenTrainingDateInPast() {

        TrainerWorkloadRequest request = new TrainerWorkloadRequest();
        request.setUsername("trainer1");
        request.setTrainingId("training1");
        request.setTrainingDurationMinutes(60);
        request.setTrainingDate(LocalDate.now().minusDays(1));
        request.setActionType(ActionType.ADD);

        try {
            service.applyEvent("event1", request);
        } catch (IllegalArgumentException ex) {
            assert ex.getMessage().equals("Event cannot be in the past");
        }
    }
}