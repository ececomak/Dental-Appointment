package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.AppointmentTreatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentTreatmentRepository extends JpaRepository<AppointmentTreatment, Long> {

    List<AppointmentTreatment> findByAppointment_IdIn(List<Long> appointmentIds);

    List<AppointmentTreatment> findByAppointment_Id(Long appointmentId);
}