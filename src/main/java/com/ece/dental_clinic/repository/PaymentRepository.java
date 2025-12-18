package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoice_IdOrderByPaymentDatetimeDesc(Long invoiceId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoice.id = :invoiceId and p.paymentStatus = 'SUCCESS'")
    BigDecimal sumByInvoiceId(@Param("invoiceId") Long invoiceId);
}