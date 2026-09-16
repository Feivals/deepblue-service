package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
    void shouldFindRescueCaseByCode() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Playa Salguero",
                RescueStatus.ADMITTED
        );

        RescueCaseResponse response = new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Playa Salguero",
                RescueStatus.ADMITTED,
                "CTR-001",
                "AN-001"
        );

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        when(mapper.toResponse(rescueCase))
                .thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);

        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCaseDoesNotExist() {

        when(repository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldChangeStatusWhenTransitionIsValid() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Playa Salguero",
                RescueStatus.ADMITTED
        );

        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION);

        RescueCaseResponse response = new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Playa Salguero",
                RescueStatus.UNDER_EVALUATION,
                "CTR-001",
                "AN-001"
        );

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        when(repository.save(rescueCase))
                .thenReturn(rescueCase);

        when(mapper.toResponse(rescueCase))
                .thenReturn(response);

        RescueCaseResponse result =
                service.changeStatus("RES-001", request);

        assertThat(result).isEqualTo(response);

        verify(repository).save(rescueCase);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTransitionIsInvalid() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Playa Salguero",
                RescueStatus.ADMITTED
        );

        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus("RES-001", request))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}