package com.deepblue.rescue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepblue.rescue.domain.MedicalRecord;

public interface MedicalRecordRepository
            extends JpaRepository<MedicalRecord,Long>{

}
