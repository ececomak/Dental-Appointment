package com.ece.dental_clinic.repository;

import com.ece.dental_clinic.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}