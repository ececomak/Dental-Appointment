package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByAppointment_Id(Long appointmentId);

    boolean existsByAppointment_Id(Long appointmentId);
}