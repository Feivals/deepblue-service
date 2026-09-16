package com.deepblue.rescue.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepblue.rescue.domain.Expertise;

public interface ExpertiseRepository 
                extends JpaRepository<Expertise,Long> {

            
            Optional<Expertise> findByNameIgnoreCase(String name);

}
