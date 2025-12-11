package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
}