package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByActiveTrueOrderByNameAsc();
}