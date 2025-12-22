package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Appointment;
import com.ece.dental_clinic.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Modifying
    @Transactional
    @Query("""
        update Appointment a
           set a.status = :expired
         where a.appointmentDatetime < :now
           and a.status not in :finalStatuses
    """)
    int expirePastAppointments(@Param("now") LocalDateTime now,
                               @Param("expired") AppointmentStatus expired,
                               @Param("finalStatuses") List<AppointmentStatus> finalStatuses);

    boolean existsByDentist_IdAndAppointmentDatetimeAndStatusNotIn(
            Long dentistId,
            LocalDateTime appointmentDatetime,
            List<AppointmentStatus> statuses
    );

    List<Appointment> findByDentist_IdAndAppointmentDatetimeBetweenAndStatusNotInOrderByAppointmentDatetimeAsc(
            Long dentistId,
            LocalDateTime start,
            LocalDateTime end,
            List<AppointmentStatus> statuses
    );

    @Query("""
      select a from Appointment a
      where a.patient.userAccount.email = :email
        and a.archivedAt is null
        and a.appointmentDatetime >= coalesce(:fromDate, a.appointmentDatetime)
        and a.appointmentDatetime <= coalesce(:toDate,   a.appointmentDatetime)
        and a.status = coalesce(:status, a.status)
        and a.dentist.id = coalesce(:dentistId, a.dentist.id)
      order by a.appointmentDatetime desc
    """)
    Page<Appointment> pagePatientAppointments(
            @Param("email") String email,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") AppointmentStatus status,
            @Param("dentistId") Long dentistId,
            Pageable pageable
    );

    @Query(
            value = """
            select a.*
            from appointment a
            join dentist d on d.id = a.dentist_id
            join user_account ua on ua.id = d.user_account_id
            join patient p on p.id = a.patient_id
            where ua.email = :email
              and a.archived_at is null
              and a.appointment_datetime >= coalesce(:fromDate, a.appointment_datetime)
              and a.appointment_datetime <= coalesce(:toDate,   a.appointment_datetime)
              and (:status is null or a.status = cast(:status as varchar))
              and (
                    :patientName is null
                 or :patientName = ''
                 or lower(p.first_name || ' ' || p.last_name)
                       like lower('%' || cast(:patientName as text) || '%')
              )
            order by a.appointment_datetime desc
        """,
            countQuery = """
            select count(*)
            from appointment a
            join dentist d on d.id = a.dentist_id
            join user_account ua on ua.id = d.user_account_id
            join patient p on p.id = a.patient_id
            where ua.email = :email
              and a.archived_at is null
              and a.appointment_datetime >= coalesce(:fromDate, a.appointment_datetime)
              and a.appointment_datetime <= coalesce(:toDate,   a.appointment_datetime)
              and (:status is null or a.status = cast(:status as varchar))
              and (
                    :patientName is null
                 or :patientName = ''
                 or lower(p.first_name || ' ' || p.last_name)
                       like lower('%' || cast(:patientName as text) || '%')
              )
        """,
            nativeQuery = true
    )
    Page<Appointment> pageDentistAppointments(
            @Param("email") String email,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") String status,
            @Param("patientName") String patientName,
            Pageable pageable
    );
}