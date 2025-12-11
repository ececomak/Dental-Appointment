package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DentistRepository extends JpaRepository<Dentist, Long> {
}