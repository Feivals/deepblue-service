# DeepBlue Rescue

Proyecto de persistencia para una plataforma de rescate y rehabilitación de fauna marina. Implementa el modelo relacional, las entidades JPA, las migraciones de esquema con Flyway y las pruebas de integración contra PostgreSQL real con Testcontainers.

## Tecnologías

- Java 21
- Spring Boot 4.1
- Spring Data JPA e Hibernate
- PostgreSQL
- Flyway
- JUnit 5 y Testcontainers

## Modelo de datos

```text
rescue_centers
  └── rescue_cases
        └── animals
              ├── medical_records
              └── treatments ── specialists
                                  └── specialist_expertise ── expertise
```

| Entidad | Descripción |
|---|---|
| `RescueCenter` | Centro que registra y atiende casos de rescate. |
| `RescueCase` | Caso de rescate, con fecha, ubicación y estado. |
| `Animal` | Animal asociado a un caso; puede tener un código de dispositivo GPS. |
| `MedicalRecord` | Expediente médico inicial del animal. |
| `Specialist` | Profesional que participa en tratamientos. |
| `Expertise` | Área de experiencia de un especialista. |
| `Treatment` | Tratamiento aplicado a un animal por un especialista. |

El campo opcional `animals.tracking_device_code` admite `NULL`; cuando tiene valor, la base garantiza que sea único.

## Relaciones

- Un `RescueCenter` tiene muchos `RescueCase`; cada caso pertenece a un centro (`1:N`).
- Un `RescueCase` tiene un `Animal` y cada animal pertenece a un caso (`1:1`).
- Un `Animal` tiene un `MedicalRecord` (`1:1`).
- Un `Animal` puede tener muchos `Treatment`; cada tratamiento corresponde a un animal (`1:N`).
- Un `Specialist` puede realizar muchos `Treatment`; cada tratamiento lo realiza un especialista (`1:N`).
- `Specialist` y `Expertise` se relacionan mediante `specialist_expertise` (`N:M`).

Las relaciones de lectura se cargan de forma perezosa (`LAZY`) donde corresponde. La creación del animal y de su expediente médico se propaga desde el caso mediante `CascadeType.ALL`.

## Ejecutar la aplicación

### Requisitos

- JDK 21
- PostgreSQL accesible (por defecto en `localhost:5432`)

La aplicación usa estas variables de entorno, con valores predeterminados:

| Variable | Valor predeterminado |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/deepblue` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Ejemplo:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/deepblue
export DB_USER=postgres
export DB_PASSWORD=postgres
./mvnw spring-boot:run
```

Al iniciar, Flyway crea o actualiza el esquema y Hibernate lo valida. La propiedad `spring.jpa.hibernate.ddl-auto` está configurada como `validate`: Hibernate no crea ni modifica tablas.

## Ejecutar las pruebas

Las pruebas de integración usan PostgreSQL mediante Testcontainers, por lo que requieren Docker en ejecución. No necesitan una base de datos local.

```bash
./mvnw clean test
```

También se puede ejecutar solamente la prueba de persistencia:

```bash
./mvnw test -Dtest=PersistenceIntegrationTest
```

## Flyway

Flyway es el responsable exclusivo de la evolución del esquema. Las migraciones se encuentran en `src/main/resources/db/migration` y se ejecutan en orden de versión:

| Migración | Propósito |
|---|---|
| `V1__create_schema.sql` | Crea tablas, claves primarias, claves foráneas, índices y constraints. |
| `V2__insert_expertise_catalog.sql` | Inserta el catálogo inicial de áreas de experiencia. |
| `V3__add_tracking_device_to_animal.sql` | Agrega `tracking_device_code` nullable y único a `animals`. |

Las migraciones ya aplicadas no se editan: un cambio posterior se expresa en una nueva versión. Con `ddl-auto: validate`, un nombre de columna incorrecto en una entidad provoca un error de validación al iniciar, en vez de una modificación implícita del esquema.

## Testcontainers

Testcontainers inicia un contenedor temporal de `postgres:18-alpine` para las pruebas. La anotación `@ServiceConnection` conecta automáticamente Spring Boot con ese contenedor; Flyway aplica las migraciones y JPA valida el esquema antes de ejecutar las pruebas.

Cada prueba de `PersistenceIntegrationTest` es transaccional, por lo que sus datos se revierten al terminar. Esto permite probar constraints reales de PostgreSQL, como `UNIQUE`, `FOREIGN KEY` y `CHECK`, de manera aislada.

## Query Methods implementados

| Repository | Método |
|---|---|
| `RescueCenterRepository` | `findByCode` |
| `RescueCaseRepository` | `findByCaseCode` |
| `RescueCaseRepository` | `findByStatusOrderByRescueDateAsc` |
| `RescueCaseRepository` | `findByRescueCenterCode` |
| `RescueCaseRepository` | `findByRescueDateAfterOrderByRescueDateDesc` |
| `AnimalRepository` | `findByAnimalCode` |
| `AnimalRepository` | `findByCommonNameContainingIgnoreCase` |
| `AnimalRepository` | `findByRescueCaseStatus` |
| `AnimalRepository` | `findByRescueCaseRescueCenterCode` |
| `ExpertiseRepository` | `findByNameIgnoreCase` |
| `TreatmentRepository` | `findByAnimalIdOrderByPerformedAtAsc` |

Los repositories también heredan operaciones CRUD de `JpaRepository`, como `findById`, `save`, `saveAndFlush`, `existsById`, `count` y `deleteById`.

## Consultas JPQL implementadas

| Repository | Método | Propósito |
|---|---|---|
| `SpecialistRepository` | `findActiveByExpertise` | Especialistas activos por área de experiencia, ordenados por apellido y nombre. |
| `AnimalRepository` | `findByRescueStatusAndSpecialistExpertise` | Animales en un estado dado tratados por especialistas con una experiencia dada. |
| `TreatmentRepository` | `findTreatmentsBetween` | Tratamientos realizados dentro de un intervalo de fechas. |
| `TreatmentRepository` | `findByCenterCode` | Tratamientos de animales pertenecientes a un centro. |
| `TreatmentRepository` | `findBySpecialistExpertise` | Tratamientos realizados por especialistas con una experiencia dada. |
