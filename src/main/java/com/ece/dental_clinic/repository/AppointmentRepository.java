package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Appointment;
import com.ece.dental_clinic.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatient_UserAccount_EmailOrderByAppointmentDatetimeDesc(String email);

    List<Appointment> findByDentist_UserAccount_EmailOrderByAppointmentDatetimeDesc(String email);

    @Modifying
    @Transactional
    @Query("""
        update Appointment a
           set a.status = :expired
         where a.appointmentDatetime < :now
           and a.status not in :finalStatuses
    """)
    int expirePastAppointments(LocalDateTime now,
                               AppointmentStatus expired,
                               List<AppointmentStatus> finalStatuses);
}