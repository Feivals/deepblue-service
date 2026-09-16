package com.deepblue.rescue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;

import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;



@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    RescueCenterRepository rescueCenterRepository;

    @Autowired
    RescueCaseRepository rescueCaseRepository;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    SpecialistRepository specialistRepository;

    @Autowired
    ExpertiseRepository expertiseRepository;

    @Autowired
    TreatmentRepository treatmentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayAppliesV1V2AndV3Migrations() {
        List<String> appliedVersions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true",
                String.class
        );

        assertTrue(appliedVersions.contains("1"));
        assertTrue(appliedVersions.contains("2"));
        assertTrue(appliedVersions.contains("3"));
        printResult("Migraciones Flyway", appliedVersions);
    }

    @Test
    void inheritedCrudMethodsPersistAndFindRescueCenter() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta")
        );

        assertNotNull(center.getId());
        assertTrue(rescueCenterRepository.existsById(center.getId()));
        assertEquals(center, rescueCenterRepository.findById(center.getId()).orElseThrow());
        assertEquals(1, rescueCenterRepository.count());
        printResult("findById(DB-CAR)", rescueCenterRepository.findById(center.getId())
                .map(found -> found.getCode() + " | " + found.getName() + " | " + found.getCity())
                .orElseThrow());
    }

    @Test
    void rescueCasesBelongToTheSameCenter() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-49", "Caribbean Center", "Santa Marta"));
        RescueCase firstCase = rescueCaseRepository.save(newRescueCase("RES-49-001", center, RescueStatus.ADMITTED));
        RescueCase secondCase = rescueCaseRepository.save(newRescueCase("RES-49-002", center, RescueStatus.UNDER_EVALUATION));

        assertSame(center, firstCase.getRescueCenter());
        assertSame(center, secondCase.getRescueCenter());
        assertEquals(center.getId(), firstCase.getRescueCenter().getId());
        assertEquals(center.getId(), secondCase.getRescueCenter().getId());
        printResult("Casos del centro " + center.getCode(), List.of(firstCase.getCaseCode(), secondCase.getCaseCode()));
    }

    @Test
    void rescueCaseAndAnimalHaveBidirectionalOneToOneRelationship() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-50", "Caribbean Center", "Santa Marta"));
        RescueCase rescueCase = newRescueCase("RES-2026-001", center, RescueStatus.ADMITTED);
        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);

        RescueCase savedCase = rescueCaseRepository.saveAndFlush(rescueCase);

        assertNotNull(savedCase.getAnimal().getId());
        assertSame(savedCase, savedCase.getAnimal().getRescueCase());
        assertEquals("AN-2026-001", savedCase.getAnimal().getAnimalCode());
        printResult("Caso y animal 1:1", savedCase.getCaseCode() + " <-> " + savedCase.getAnimal().getAnimalCode());
    }

    @Test
    void savingAnimalCascadesItsMedicalRecord() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-51", "Caribbean Center", "Santa Marta"));
        RescueCase rescueCase = rescueCaseRepository.save(newRescueCase("RES-2026-002", center, RescueStatus.ADMITTED));
        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);
        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"), "STABLE", "Left front flipper injury", null
        );
        animal.assignMedicalRecord(record);

        Animal savedAnimal = animalRepository.saveAndFlush(animal);

        assertNotNull(savedAnimal.getId());
        assertNotNull(savedAnimal.getMedicalRecord().getId());
        assertSame(savedAnimal, savedAnimal.getMedicalRecord().getAnimal());
        printResult("Expediente medico", savedAnimal.getAnimalCode() + " | peso="
                + savedAnimal.getMedicalRecord().getInitialWeight() + " | condicion="
                + savedAnimal.getMedicalRecord().getInitialCondition());
    }

    @Test
    void specialistHasTwoExpertiseAreas() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Specialist elena = new Specialist("SP-52-ELENA", "Elena", "Vargas", "elena.52@example.com", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist savedSpecialist = specialistRepository.saveAndFlush(elena);

        assertEquals(2, savedSpecialist.getExpertiseAreas().size());
        assertTrue(savedSpecialist.getExpertiseAreas().containsAll(List.of(trauma, rehabilitation)));
        printResult("Especialidades de Elena", savedSpecialist.getExpertiseAreas().stream()
                .map(Expertise::getName).sorted().toList());
    }

    @Test
    void findsOnlyCasesInRehabilitation() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-53", "Caribbean Center", "Santa Marta"));
        rescueCaseRepository.save(newRescueCase("RES-001", center, RescueStatus.IN_REHABILITATION));
        rescueCaseRepository.save(newRescueCase("RES-002", center, RescueStatus.READY_FOR_RELEASE));
        rescueCaseRepository.save(newRescueCase("RES-003", center, RescueStatus.IN_REHABILITATION));

        List<RescueCase> cases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertEquals(2, cases.size());
        assertTrue(cases.stream().allMatch(rescueCase -> rescueCase.getStatus() == RescueStatus.IN_REHABILITATION));
        printResult("Casos IN_REHABILITATION", cases.stream().map(RescueCase::getCaseCode).toList());
    }

    @Test
    void findsAnimalsByTheirRescueCenterCode() {
        RescueCenter caribbean = rescueCenterRepository.save(new RescueCenter("DB-CAR", "Caribbean Center", "Santa Marta"));
        RescueCenter pacific = rescueCenterRepository.save(new RescueCenter("DB-PAC", "Pacific Center", "Buenaventura"));
        Animal caribbeanAnimal = saveAnimal("AN-CAR-54", newRescueCase("RES-CAR-54", caribbean, RescueStatus.ADMITTED));
        saveAnimal("AN-PAC-54", newRescueCase("RES-PAC-54", pacific, RescueStatus.ADMITTED));

        List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertEquals(1, animals.size());
        assertEquals(caribbeanAnimal.getId(), animals.getFirst().getId());
        assertTrue(animals.stream().allMatch(animal -> "DB-CAR".equals(animal.getRescueCase().getRescueCenter().getCode())));
        printResult("Animales de DB-CAR", animals.stream()
                .map(animal -> animal.getAnimalCode() + " | " + animal.getCommonName()).toList());
    }

    @Test
    void findsActiveSpecialistsByExpertiseUsingJpql() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();
        Specialist elena = saveSpecialist("SP-55-ELENA", "Elena", "Vargas", trauma, rehabilitation);
        Specialist mateo = saveSpecialist("SP-55-MATEO", "Mateo", "Rios", marineMammals, rehabilitation);
        Specialist sofia = saveSpecialist("SP-55-SOFIA", "Sofia", "Alvarez", marineBirds, trauma);

        List<Specialist> specialists = specialistRepository.findActiveByExpertise("Trauma");

        assertIterableEquals(List.of(sofia.getId(), elena.getId()), specialists.stream().map(Specialist::getId).toList());
        assertFalse(specialists.contains(mateo));
        printResult("Especialistas activos con Trauma", specialists.stream()
                .map(specialist -> specialist.getFirstName() + " " + specialist.getLastName()).toList());
    }

    @Test
    void findsTreatmentsForAnimalInChronologicalOrder() {
        Animal animal = animalForTreatments("56");
        Specialist elena = saveSpecialist("SP-56-ELENA", "Elena", "Vargas");
        Specialist mateo = saveSpecialist("SP-56-MATEO", "Mateo", "Rios");
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Treatment 1"));
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 2, 10, 0), TreatmentType.HYDRATION, "Treatment 2"));
        treatmentRepository.saveAndFlush(new Treatment(animal, mateo, LocalDateTime.of(2026, 8, 3, 10, 0), TreatmentType.OBSERVATION, "Treatment 3"));

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertIterableEquals(List.of("Treatment 1", "Treatment 2", "Treatment 3"),
                treatments.stream().map(Treatment::getDescription).toList());
        printResult("Tratamientos del animal " + animal.getAnimalCode(), treatments.stream()
                .map(treatment -> treatment.getPerformedAt() + " | " + treatment.getDescription()).toList());
    }

    @Test
    void findsOnlyTreatmentsWithinTheRequestedInterval() {
        Animal animal = animalForTreatments("58");
        Specialist elena = saveSpecialist("SP-58-ELENA", "Elena", "Vargas");
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Before interval"));
        Treatment expectedTreatment = treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Inside interval"));
        treatmentRepository.saveAndFlush(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "After interval"));

        List<Treatment> treatments = treatmentRepository.findTreatmentsBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59, 59)
        );

        assertEquals(1, treatments.size());
        assertEquals(expectedTreatment.getId(), treatments.getFirst().getId());
        assertEquals(LocalDateTime.of(2026, 8, 10, 10, 0), treatments.getFirst().getPerformedAt());
        printResult("Tratamientos entre 2026-08-05 y 2026-08-15", treatments.stream()
                .map(treatment -> treatment.getPerformedAt() + " | " + treatment.getDescription()).toList());
    }

    @Test
    void rejectsDuplicateAnimalCode() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-59", "Caribbean Center", "Santa Marta"));
        saveAnimal("AN-100", newRescueCase("RES-59-001", center, RescueStatus.ADMITTED));

        RescueCase secondCase = rescueCaseRepository.save(
                newRescueCase("RES-59-002", center, RescueStatus.ADMITTED)
        );
        Animal duplicateAnimal = new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        duplicateAnimal.setRescueCase(secondCase);

        assertThrows(DataIntegrityViolationException.class,
                () -> animalRepository.saveAndFlush(duplicateAnimal));
    }

    @Test
    void postgresRejectsRescueCaseWithUnknownRescueCenter() {
        /*
         * JPA prevents the usual object-model mistake before SQL is issued: a
         * RescueCase requires a non-null RescueCenter because of nullable = false.
         * The FK remains necessary to protect the table from SQL or another client
         * that supplies an id which does not exist.
         */
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into rescue_cases "
                        + "(case_code, rescue_date, rescue_location, status, rescue_center_id) "
                        + "values (?, ?, ?, ?, ?)",
                "RES-60-INVALID-FK",
                LocalDate.of(2026, 1, 15),
                "Santa Marta",
                "ADMITTED",
                -1L
        ));
    }

    @Test
    void postgresRejectsStatusOutsideTheEnumValues() {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-61", "Caribbean Center", "Santa Marta"));
        RescueCase rescueCase = rescueCaseRepository.saveAndFlush(
                newRescueCase("RES-61-001", center, RescueStatus.ADMITTED)
        );

        /*
         * Java's enum protects values created through this application. The CHECK
         * constraint independently protects PostgreSQL from direct SQL, other
         * services, imports, and future application defects.
         */
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "update rescue_cases set status = ? where id = ?",
                "INVALID_STATUS",
                rescueCase.getId()
        ));
    }

    @Test
    void persistsDeepBlueCaribbeanScenarioAndAnswersItsQueries() {
        RescueCenter center = rescueCenterRepository.saveAndFlush(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        animal.assignMedicalRecord(new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
        ));
        rescueCase.assignAnimal(animal);
        RescueCase savedCase = rescueCaseRepository.saveAndFlush(rescueCase);

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow());
        elena.addExpertise(expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow());
        elena.addExpertise(expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow());
        Specialist savedSpecialist = specialistRepository.saveAndFlush(elena);

        Treatment woundCare = treatmentRepository.save(new Treatment(
                savedCase.getAnimal(), savedSpecialist, LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper"
        ));
        Treatment hydration = treatmentRepository.saveAndFlush(new Treatment(
                savedCase.getAnimal(), savedSpecialist, LocalDateTime.of(2026, 8, 19, 10, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy"
        ));

        // Consulta 1: Query Method por código de caso.
        assertTrue(rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent());
        // Consulta 2: Query Method por estado.
        assertTrue(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION)
                .stream().anyMatch(found -> found.getId().equals(savedCase.getId())));
        // Consultas 3 y 4: Query Methods por navegación y nombre común.
        assertTrue(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR")
                .stream().anyMatch(found -> found.getId().equals(savedCase.getAnimal().getId())));
        assertTrue(animalRepository.findByCommonNameContainingIgnoreCase("turtle")
                .stream().anyMatch(found -> found.getId().equals(savedCase.getAnimal().getId())));
        // Consulta 5: JPQL por área de experiencia.
        assertTrue(specialistRepository.findActiveByExpertise("Trauma")
                .stream().anyMatch(found -> found.getId().equals(savedSpecialist.getId())));
        // Consulta 6: Query Method cronológico por animal.
        assertIterableEquals(List.of(woundCare.getId(), hydration.getId()),
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(savedCase.getAnimal().getId())
                        .stream().map(Treatment::getId).toList());
        // Consulta 7: JPQL por experiencia del especialista.
        assertIterableEquals(List.of(woundCare.getId(), hydration.getId()),
                treatmentRepository.findBySpecialistExpertise("Rehabilitation")
                        .stream().map(Treatment::getId).toList());
        // Consulta 8: JPQL para el intervalo de fechas.
        assertIterableEquals(List.of(woundCare.getId(), hydration.getId()),
                treatmentRepository.findTreatmentsBetween(
                                LocalDateTime.of(2026, 8, 19, 0, 0),
                                LocalDateTime.of(2026, 8, 19, 23, 59, 59)
                        )
                        .stream().map(Treatment::getId).toList());
        assertEquals(new BigDecimal("27.80"), savedCase.getAnimal().getMedicalRecord().getInitialWeight());
    }

    @Test
    void findsAnimalsInRehabilitationTreatedByTraumaSpecialists() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-TRAUMA", "Trauma Center", "Santa Marta")
        );
        Animal eligibleAnimal = saveAnimal("AN-TRAUMA-01",
                newRescueCase("RES-TRAUMA-01", center, RescueStatus.IN_REHABILITATION));
        Animal admittedAnimal = saveAnimal("AN-TRAUMA-02",
                newRescueCase("RES-TRAUMA-02", center, RescueStatus.ADMITTED));
        Specialist traumaSpecialist = saveSpecialist("SP-TRAUMA", "Elena", "Vargas",
                expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow());

        treatmentRepository.save(new Treatment(eligibleAnimal, traumaSpecialist,
                LocalDateTime.of(2026, 8, 20, 9, 0), TreatmentType.WOUND_CARE, "First trauma treatment"));
        treatmentRepository.save(new Treatment(eligibleAnimal, traumaSpecialist,
                LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Second trauma treatment"));
        treatmentRepository.saveAndFlush(new Treatment(admittedAnimal, traumaSpecialist,
                LocalDateTime.of(2026, 8, 20, 11, 0), TreatmentType.WOUND_CARE, "Admitted animal treatment"));

        List<Animal> animals = animalRepository.findByRescueStatusAndSpecialistExpertise(
                RescueStatus.IN_REHABILITATION, "tRaUmA"
        );

        assertIterableEquals(List.of(eligibleAnimal.getId()), animals.stream().map(Animal::getId).toList());
    }

    @Test
    void findsAnimalsContainingCommonName() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-COMMON-NAME", "Caribbean Center", "Santa Marta")
        );
        Animal turtle = saveAnimal("AN-COMMON-NAME",
                newRescueCase("RES-COMMON-NAME", center, RescueStatus.ADMITTED));

        List<Animal> animals = animalRepository.findByCommonNameContainingIgnoreCase("turtle");

        assertTrue(animals.stream().anyMatch(animal -> animal.getId().equals(turtle.getId())));
    }

    private RescueCase newRescueCase(String code, RescueCenter center, RescueStatus status) {
        RescueCase rescueCase = new RescueCase(code, LocalDate.of(2026, 1, 15), "Santa Marta", status);
        rescueCase.setRescueCenter(center);
        return rescueCase;
    }

    private Animal saveAnimal(String code, RescueCase rescueCase) {
        RescueCase savedCase = rescueCaseRepository.save(rescueCase);
        Animal animal = new Animal(code, "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        animal.setRescueCase(savedCase);
        return animalRepository.saveAndFlush(animal);
    }

    private Animal animalForTreatments(String suffix) {
        RescueCenter center = rescueCenterRepository.save(new RescueCenter("DB-CAR-" + suffix, "Caribbean Center", "Santa Marta"));
        return saveAnimal("AN-" + suffix, newRescueCase("RES-" + suffix, center, RescueStatus.IN_REHABILITATION));
    }

    private Specialist saveSpecialist(String code, String firstName, String lastName, Expertise... expertiseAreas) {
        Specialist specialist = new Specialist(code, firstName, lastName, code.toLowerCase() + "@example.com", true);
        for (Expertise expertise : expertiseAreas) {
            specialist.addExpertise(expertise);
        }
        return specialistRepository.saveAndFlush(specialist);
    }

    private void printResult(String query, Object result) {
        System.out.println("\n>>> " + query + ": " + result);
    }

}
