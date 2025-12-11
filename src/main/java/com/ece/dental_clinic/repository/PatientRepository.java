package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}