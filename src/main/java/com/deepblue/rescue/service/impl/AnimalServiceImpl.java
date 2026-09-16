package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.AnimalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository repository;
    private final AnimalMapper mapper;

    public AnimalServiceImpl(AnimalRepository repository, AnimalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AnimalResponse findByCode(String animalCode) {
        return findAnimalByCode(animalCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + animalCode
                ));
    }

    @Override
    public List<AnimalResponse> findAnimalsInRehabilitation() {
        return repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public boolean canReceiveTreatment(String animalCode) {
        return findAnimalByCode(animalCode)
                .map(animal -> animal.getRescueCase().getStatus())
                .map(status -> status == RescueStatus.UNDER_EVALUATION
                        || status == RescueStatus.IN_REHABILITATION)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + animalCode
                ));
    }

    private java.util.Optional<Animal> findAnimalByCode(String animalCode) {
        return repository.findByAnimalCode(animalCode);
    }
}
