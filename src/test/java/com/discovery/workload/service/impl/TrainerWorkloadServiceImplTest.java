package com.discovery.workload.service.impl;

import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.entity.ProcessedEventEntity;
import com.discovery.workload.entity.TrainerMonthKey;
import com.discovery.workload.entity.TrainerMonthlySummary;
import com.discovery.workload.exception.NotFoundException;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.model.MonthlySummary;
import com.discovery.workload.model.TrainerYearlySummary;
import com.discovery.workload.repository.ProcessedEventRepository;
import com.discovery.workload.repository.TrainerMonthlySummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceImplTest {

    @Mock
    private TrainerMonthlySummaryRepository repository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    private TrainerWorkloadRequest baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = TrainerWorkloadRequest.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingId("t-1")
                .trainingDate(LocalDate.now().plusDays(1))
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();
    }

    @Test
    void applyEvent_missingEventId_generatesAndSaves() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(repository.findById(any(TrainerMonthKey.class))).thenReturn(Optional.empty());
        when(repository.save(any(TrainerMonthlySummary.class))).thenAnswer(inv -> inv.getArgument(0));
        when(processedEventRepository.save(any(ProcessedEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.applyEvent("   ", baseRequest));

        verify(processedEventRepository).existsById(anyString());
        verify(repository).save(any(TrainerMonthlySummary.class));
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void applyEvent_duplicateEventId_returnsAndDoesNotSaveSummaryOrProcessedEvent() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        assertDoesNotThrow(() -> service.applyEvent("evt-1", baseRequest));

        verify(processedEventRepository).existsById("evt-1");
        verifyNoInteractions(repository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void applyEvent_pastDate_throwsIllegalArgumentException() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        TrainerWorkloadRequest req = TrainerWorkloadRequest.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingId("t-1")
                .trainingDate(LocalDate.now().minusDays(1)) // past
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.applyEvent("evt-1", req)
        );
        assertEquals("Event cannot be in the past", ex.getMessage());

        verify(processedEventRepository).existsById("evt-1");
        verifyNoInteractions(repository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void applyEvent_addAction_createsNewSummaryAndAddsDuration() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);
        when(repository.findById(any(TrainerMonthKey.class))).thenReturn(Optional.empty());

        ArgumentCaptor<TrainerMonthlySummary> summaryCaptor = ArgumentCaptor.forClass(TrainerMonthlySummary.class);
        when(repository.save(summaryCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.applyEvent("evt-1", baseRequest));

        TrainerMonthlySummary saved = summaryCaptor.getValue();
        assertEquals("john", saved.getId().getUsername());
        assertEquals(baseRequest.getTrainingDate().getYear(), saved.getId().getYear());
        assertEquals(baseRequest.getTrainingDate().getMonthValue(), saved.getId().getMonth());
        assertEquals("John", saved.getFirstName());
        assertEquals("Doe", saved.getLastName());
        assertTrue(saved.isActive());
        assertEquals(60, saved.getTotalDurationMinutes());

        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void applyEvent_subtractAction_updatesExistingSummaryAndSubtractsDuration() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        LocalDate date = baseRequest.getTrainingDate();
        TrainerMonthKey key = new TrainerMonthKey("john", date.getYear(), date.getMonthValue());

        TrainerMonthlySummary existing = TrainerMonthlySummary.builder()
                .id(key)
                .firstName("Old")
                .lastName("Name")
                .active(true)
                .totalDurationMinutes(200)
                .build();

        when(repository.findById(key)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrainerWorkloadRequest req = TrainerWorkloadRequest.builder()
                .username("john")
                .firstName("John") // update info
                .lastName("Doe")
                .isActive(false)
                .trainingId("t-2")
                .trainingDate(date)
                .trainingDurationMinutes(50)
                .actionType(ActionType.DELETE)
                .build();

        assertDoesNotThrow(() -> service.applyEvent("evt-1", req));

        assertEquals("John", existing.getFirstName());
        assertEquals("Doe", existing.getLastName());
        assertFalse(existing.isActive());
        assertEquals(150, existing.getTotalDurationMinutes()); // 200 - 50

        verify(repository).save(existing);
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void getMonthlySummary_found_returnsModel() {
        TrainerMonthKey key = new TrainerMonthKey("john", 2026, 2);

        TrainerMonthlySummary row = TrainerMonthlySummary.builder()
                .id(key)
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .totalDurationMinutes(123)
                .build();

        when(repository.findById(key)).thenReturn(Optional.of(row));

        MonthlySummary res = service.getMonthlySummary("john", 2026, 2);

        assertEquals("john", res.getUsername());
        assertEquals("John", res.getFirstName());
        assertEquals("Doe", res.getLastName());
        assertTrue(res.isActive());
        assertEquals(2026, res.getYear());
        assertEquals(2, res.getMonth());
        assertEquals(123, res.getTotalDurationMinutes());
    }

    @Test
    void getMonthlySummary_notFound_throwsNotFoundException() {
        TrainerMonthKey key = new TrainerMonthKey("john", 2026, 2);
        when(repository.findById(key)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getMonthlySummary("john", 2026, 2));
    }

    @Test
    void getTrainerSummary_empty_throwsNotFoundException() {
        when(repository.findAllByIdUsernameOrderByIdYearAscIdMonthAsc("john"))
                .thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.getTrainerSummary("john"));
    }

    @Test
    void getTrainerSummary_groupsYearsAndSortsMonths_andUsesShortMonthName() {
        TrainerMonthlySummary jan2026 = TrainerMonthlySummary.builder()
                .id(new TrainerMonthKey("john", 2026, 1))
                .firstName("John").lastName("Doe").active(true)
                .totalDurationMinutes(10)
                .build();

        TrainerMonthlySummary mar2026 = TrainerMonthlySummary.builder()
                .id(new TrainerMonthKey("john", 2026, 3))
                .firstName("John").lastName("Doe").active(true)
                .totalDurationMinutes(30)
                .build();

        TrainerMonthlySummary feb2025 = TrainerMonthlySummary.builder()
                .id(new TrainerMonthKey("john", 2025, 2))
                .firstName("John").lastName("Doe").active(true)
                .totalDurationMinutes(20)
                .build();

        when(repository.findAllByIdUsernameOrderByIdYearAscIdMonthAsc("john"))
                .thenReturn(List.of(feb2025, mar2026, jan2026));

        TrainerYearlySummary res = service.getTrainerSummary("john");

        assertEquals("john", res.getTrainerUsername());
        assertEquals("John", res.getTrainerFirstName());
        assertEquals("Doe", res.getTrainerLastName());
        assertEquals("ACTIVE", res.getTrainerStatus());

        assertEquals(2, res.getYears().size());

        TrainerYearlySummary.Year y2025 = res.getYears().get(0);
        assertEquals(2025, y2025.getYear());
        assertEquals(1, y2025.getMonths().size());
        assertEquals("Feb", y2025.getMonths().get(0).getMonth());
        assertEquals(20, y2025.getMonths().get(0).getTrainingSummaryDurationMinutes());

        TrainerYearlySummary.Year y2026 = res.getYears().get(1);
        assertEquals(2026, y2026.getYear());

        assertEquals(2, y2026.getMonths().size());
        assertEquals("Jan", y2026.getMonths().get(0).getMonth());
        assertEquals(10, y2026.getMonths().get(0).getTrainingSummaryDurationMinutes());

        assertEquals("Mar", y2026.getMonths().get(1).getMonth());
        assertEquals(30, y2026.getMonths().get(1).getTrainingSummaryDurationMinutes());
    }
}
