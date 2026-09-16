package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;   // 👈 necesario para verify(..., never())

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    void shouldRegisterTreatmentWhenRequestIsValid() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                "Playa Salguero",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",              // animalCode
                "Green Sea Turtle",    // commonName
                "Chelonia mydas",      // scientificName
                AnimalSex.FEMALE,       // sex
                rescueCase
        );

        Specialist specialist = new Specialist(
                "SPEC-001",              // professionalCode
                "Elena",                 // firstName
                "Vargas",                // lastName
                "elena.vargas@deepblue.com", // email (inventa uno o usa null)
                true                     // active
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 10, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        Treatment treatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description()
        );

        TreatmentResponse response = new TreatmentResponse(
                1L,
                "AN-001",
                "SPEC-001",
                request.performedAt(),
                request.type(),
                request.description()
        );

        when(
                animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(
                Optional.of(animal)
        );

        when(
                specialistRepository.findByProfessionalCode("SPEC-001")
        ).thenReturn(
                Optional.of(specialist)
        );

        when(
                treatmentRepository.save(any(Treatment.class))
        ).thenReturn(treatment);

        when(
                mapper.toResponse(treatment)
        ).thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result)
                .isEqualTo(response);

        verify(treatmentRepository)
                .save(any(Treatment.class));
    }
    @Test
    void shouldThrowBusinessRuleExceptionWhenSpecialistIsInactive() {

        // ==========================================
        // ARRANGE
        // ==========================================

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                "Playa Salguero",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                rescueCase
        );
        animal.setRescueCase(rescueCase);  // 👈 Recordar del Problema 7

        // 👇 La clave de este test: specialist INACTIVO
        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena.vargas@deepblue.com",
                false                        // ❌ active = false
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 10, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        // ==========================================
        // Configurar los MOCKS
        // ==========================================

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        // ⚠️ NO configuramos treatmentRepository.save ni mapper.toResponse
        // porque el test espera que NO se llamen.

        // ==========================================
        // ACT + ASSERT
        // ==========================================

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        // ==========================================
        // VERIFY
        // ==========================================

        verify(treatmentRepository, never()).save(any(Treatment.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenRescueCaseIsReleased() {

        // ==========================================
        // ARRANGE
        // ==========================================

        // 👇 La clave: RescueCase con estado RELEASED
        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                "Playa Salguero",
                RescueStatus.RELEASED        // ❌ ya fue liberado
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                rescueCase
        );
        animal.setRescueCase(rescueCase);   // 👈 ¡Importante!

        // Specialist activo (no es el problema aquí)
        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena.vargas@deepblue.com",
                true                          // ✅ activo
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 10, 9, 0),
                TreatmentType.OBSERVATION,
                "Routine observation after release."
        );

        // ==========================================
        // Configurar los MOCKS
        // ==========================================

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        // ⚠️ NO configuramos save() ni mapper.toResponse()

        // ==========================================
        // ACT + ASSERT
        // ==========================================

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        // ==========================================
        // VERIFY
        // ==========================================

        verify(treatmentRepository, never()).save(any(Treatment.class));
        verify(mapper, never()).toResponse(any());
    }

}