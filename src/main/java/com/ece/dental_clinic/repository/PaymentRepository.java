package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoice_IdOrderByPaymentDatetimeDesc(Long invoiceId);
}