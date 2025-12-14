package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.Invoice;
import com.ece.dental_clinic.entity.Payment;
import com.ece.dental_clinic.enums.InvoiceStatus;
import com.ece.dental_clinic.repository.InvoiceRepository;
import com.ece.dental_clinic.repository.PaymentRepository;
import com.ece.dental_clinic.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class PatientInvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;

    public PatientInvoiceController(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PatientRepository patientRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/patient/invoices/{invoiceId}")
    public String viewInvoice(@PathVariable Long invoiceId, Authentication auth, Model model) {

        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura bulunamadı: " + invoiceId));

        String email = auth.getName();

        boolean isOwner = inv.getAppointment() != null
                && inv.getAppointment().getPatient() != null
                && inv.getAppointment().getPatient().getUserAccount() != null
                && email.equals(inv.getAppointment().getPatient().getUserAccount().getEmail());

        if (!isOwner) {
            throw new RuntimeException("Bu faturayı görme yetkin yok.");
        }

        model.addAttribute("invoice", inv);
        return "patient-invoice";
    }

    @PostMapping("/patient/invoices/{invoiceId}/pay")
    public String payInvoice(@PathVariable Long invoiceId, Authentication auth) {

        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura bulunamadı: " + invoiceId));

        String email = auth.getName();
        boolean isOwner = inv.getAppointment() != null
                && inv.getAppointment().getPatient() != null
                && inv.getAppointment().getPatient().getUserAccount() != null
                && email.equals(inv.getAppointment().getPatient().getUserAccount().getEmail());

        if (!isOwner) {
            throw new RuntimeException("Bu faturayı ödeme yetkin yok.");
        }

        if (inv.getStatus() == InvoiceStatus.PAID) {
            return "redirect:/patient/invoices/" + invoiceId;
        }

        Payment p = new Payment();
        p.setInvoice(inv);
        p.setAmount(BigDecimal.valueOf(inv.getFinalAmount()));
        p.setPaymentDatetime(LocalDateTime.now());
        p.setPaymentMethod("CASH");
        p.setPaymentStatus("SUCCESS");
        p.setTransactionNo("TX-" + UUID.randomUUID());

        paymentRepository.save(p);

        inv.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(inv);

        return "redirect:/patient/invoices/" + invoiceId;
    }
}