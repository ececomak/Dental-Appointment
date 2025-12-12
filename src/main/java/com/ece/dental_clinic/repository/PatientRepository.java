package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserAccount_Email(String email);
}