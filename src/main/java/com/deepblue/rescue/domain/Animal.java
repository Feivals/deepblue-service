package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true)
    private String animalCode;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "scientific_name")
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false)
    private AnimalSex sex;

    @Column(name = "tracking_device_code", unique = true)
    private String trackingDeviceCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rescue_case_id",
            nullable = false,
            unique = true
    )
    private RescueCase rescueCase;

     @OneToOne(
            mappedBy = "animal",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "animal")
    private List<Treatment> treatments = new ArrayList<>();




    public Animal() {
    }

    public Animal(String animalCode,String commonName,String scientificName,AnimalSex sex,
                  RescueCase rescueCase){
        this.animalCode = animalCode;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.sex = sex;
        this.rescueCase = rescueCase;
    }

    public Long getId() {
        return id;
    }

    public String getAnimalCode() {
        return animalCode;
    }

    public void setAnimalCode(String animalCode) {
        this.animalCode = animalCode;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public AnimalSex getSex() {
        return sex;
    }

    public void setSex(AnimalSex sex) {
        this.sex = sex;
    }

    public String getTrackingDeviceCode() {
        return trackingDeviceCode;
    }

    public void setTrackingDeviceCode(String trackingDeviceCode) {
        this.trackingDeviceCode = trackingDeviceCode;
    }

    public RescueCase getRescueCase() {
        return rescueCase;
    }

    public void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;

        if (medicalRecord != null) {
            medicalRecord.setAnimal(this);
        }
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }


    
}
