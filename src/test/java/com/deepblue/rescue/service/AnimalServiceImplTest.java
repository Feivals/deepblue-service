package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldAllowTreatmentWhenAnimalIsInRehabilitation() {
        Animal animal = animalWithStatus(RescueStatus.IN_REHABILITATION);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @Test
    void shouldAllowTreatmentWhenAnimalIsUnderEvaluation() {
        Animal animal = animalWithStatus(RescueStatus.UNDER_EVALUATION);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @Test
    void shouldRejectTreatmentWhenAnimalIsReleased() {
        Animal animal = animalWithStatus(RescueStatus.RELEASED);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
    }

    private Animal animalWithStatus(RescueStatus status) {
        RescueCase rescueCase = new RescueCase(
                "RES-001", LocalDate.of(2026, 8, 20), "Playa Salguero", status
        );
        return new Animal(
                "AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE,
                rescueCase
        );
    }
}
