package com.deepblue.rescue.dto.request;

import java.time.LocalDateTime;

import com.deepblue.rescue.domain.TreatmentType;

public record CreateTreatmentRequest(
    String animalCode,

        String specialistCode,

        LocalDateTime performedAt,

        TreatmentType type,

        String description
) {

}
