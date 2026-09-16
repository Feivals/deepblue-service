package com.deepblue.rescue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepblue.rescue.domain.Specialist;

public interface SpecialistRepository 
            extends JpaRepository<Specialist,Long> {

        @Query("""
        select distinct s
        from Specialist s
        join s.expertiseAreas e
        where s.active = true
          and lower(e.name) = lower(:expertiseName)
        order by s.lastName asc, s.firstName asc
        """)
    List<Specialist> findActiveByExpertise(
            @Param("expertiseName") String expertiseName
    );

}
