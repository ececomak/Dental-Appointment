package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
}