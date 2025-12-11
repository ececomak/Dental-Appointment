package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.AppointmentTreatment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentTreatmentRepository extends JpaRepository<AppointmentTreatment, Long> {
}