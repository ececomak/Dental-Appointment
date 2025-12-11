package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
}