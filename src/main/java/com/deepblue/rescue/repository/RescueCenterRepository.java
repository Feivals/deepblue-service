package com.deepblue.rescue.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepblue.rescue.domain.RescueCenter;

public interface RescueCenterRepository 
        extends JpaRepository<RescueCenter, Long>{

            Optional<RescueCenter> findByCode(String code);

}
