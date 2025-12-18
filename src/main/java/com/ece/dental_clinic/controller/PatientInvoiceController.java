package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.Invoice;
import com.ece.dental_clinic.entity.Payment;
import com.ece.dental_clinic.enums.InvoiceStatus;
import com.ece.dental_clinic.enums.PaymentMethod;
import com.ece.dental_clinic.repository.InvoiceRepository;
import com.ece.dental_clinic.repository.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
public class PatientInvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public PatientInvoiceController(InvoiceRepository invoiceRepository,
                                    PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/patient/invoices/{invoiceId}/pay")
    public String payInvoiceRedirect(@PathVariable Long invoiceId) {
        return "redirect:/patient/invoices/" + invoiceId;
    }

    @GetMapping("/patient/invoices/{invoiceId}")
    public String viewInvoice(@PathVariable Long invoiceId,
                              Authentication auth,
                              Model model,
                              @RequestParam(value = "err", required = false) String err) {

        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura bulunamadı: " + invoiceId));

        String email = auth.getName();
        boolean isOwner = inv.getAppointment() != null
                && inv.getAppointment().getPatient() != null
                && inv.getAppointment().getPatient().getUserAccount() != null
                && email.equals(inv.getAppointment().getPatient().getUserAccount().getEmail());

        if (!isOwner) throw new RuntimeException("Bu faturayı görme yetkin yok.");

        List<Payment> payments = paymentRepository.findByInvoice_IdOrderByPaymentDatetimeDesc(invoiceId);

        BigDecimal paidTotal = payments.stream()
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = inv.getFinalAmount() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(inv.getFinalAmount());

        BigDecimal remaining = total.subtract(paidTotal);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        boolean overdue = inv.getDueDate() != null
                && remaining.signum() > 0
                && inv.getDueDate().isBefore(LocalDate.now())
                && inv.getStatus() != InvoiceStatus.CANCELLED;

        InvoiceStatus computed = computeStatus(inv.getStatus(), paidTotal, total);
        if (computed != inv.getStatus()) {
            inv.setStatus(computed);
            invoiceRepository.save(inv);
        }

        model.addAttribute("invoice", inv);
        model.addAttribute("payments", payments);
        model.addAttribute("paidTotal", paidTotal);
        model.addAttribute("remaining", remaining);
        model.addAttribute("overdue", overdue);
        model.addAttribute("err", err);

        return "patient-invoice";
    }

    @PostMapping("/patient/invoices/{invoiceId}/pay")
    public String payInvoice(@PathVariable Long invoiceId,
                             @RequestParam(value = "amount", required = false) String amountRaw,
                             @RequestParam(value = "method", required = false) String methodRaw,
                             @RequestParam(value = "cardNumber", required = false) String cardNumber,
                             @RequestParam(value = "exp", required = false) String exp,
                             @RequestParam(value = "cvv", required = false) String cvv,
                             @RequestParam(value = "brand", required = false) String brand,
                             Authentication auth) {

        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura bulunamadı: " + invoiceId));

        String email = auth.getName();
        boolean isOwner = inv.getAppointment() != null
                && inv.getAppointment().getPatient() != null
                && inv.getAppointment().getPatient().getUserAccount() != null
                && email.equals(inv.getAppointment().getPatient().getUserAccount().getEmail());

        if (!isOwner) throw new RuntimeException("Bu faturayı ödeme yetkin yok.");
        if (inv.getStatus() == InvoiceStatus.CANCELLED) throw new RuntimeException("İptal edilmiş faturaya ödeme yapılamaz.");

        if (amountRaw == null || amountRaw.isBlank()) return "redirect:/patient/invoices/" + invoiceId + "?err=amount";

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw.replace(",", ".").trim());
        } catch (Exception e) {
            return "redirect:/patient/invoices/" + invoiceId + "?err=amount";
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return "redirect:/patient/invoices/" + invoiceId + "?err=amount";

        BigDecimal total = inv.getFinalAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(inv.getFinalAmount());
        BigDecimal paidTotal = paymentRepository.sumByInvoiceId(invoiceId);
        if (paidTotal == null) paidTotal = BigDecimal.ZERO;

        BigDecimal remaining = total.subtract(paidTotal);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        if (remaining.signum() <= 0) {
            inv.setStatus(computeStatus(inv.getStatus(), paidTotal, total));
            invoiceRepository.save(inv);
            return "redirect:/patient/invoices/" + invoiceId;
        }

        if (amount.compareTo(remaining) > 0) return "redirect:/patient/invoices/" + invoiceId + "?err=amount";

        PaymentMethod method = PaymentMethod.CASH;
        if (methodRaw != null && !methodRaw.isBlank()) {
            method = PaymentMethod.valueOf(methodRaw.trim().toUpperCase());
        }

        if (method == PaymentMethod.CARD) {
            String digits = (cardNumber == null) ? "" : cardNumber.replaceAll("\\D", "");
            if (digits.length() != 16) return "redirect:/patient/invoices/" + invoiceId + "?err=card";
            if (exp == null || !exp.matches("^(0[1-9]|1[0-2])\\/\\d{2}$")) return "redirect:/patient/invoices/" + invoiceId + "?err=exp";
            if (cvv == null || !cvv.matches("^\\d{3}$")) return "redirect:/patient/invoices/" + invoiceId + "?err=cvv";
            if (brand == null || brand.isBlank()) return "redirect:/patient/invoices/" + invoiceId + "?err=brand";

            int lastDigit = Character.getNumericValue(digits.charAt(digits.length() - 1));
            if (lastDigit % 2 != 0) return "redirect:/patient/invoices/" + invoiceId + "?err=3ds";
        }

        Payment p = new Payment();
        p.setInvoice(inv);
        p.setAmount(amount);
        p.setPaymentDatetime(LocalDateTime.now());
        p.setPaymentMethod(method);
        p.setPaymentStatus("SUCCESS");
        p.setTransactionNo("TX-" + UUID.randomUUID());
        paymentRepository.save(p);

        BigDecimal newPaidTotal = paidTotal.add(amount);
        inv.setStatus(computeStatus(inv.getStatus(), newPaidTotal, total));
        invoiceRepository.save(inv);

        return "redirect:/patient/invoices/" + invoiceId;
    }

    private InvoiceStatus computeStatus(InvoiceStatus current, BigDecimal paid, BigDecimal total) {
        if (current == InvoiceStatus.CANCELLED) return InvoiceStatus.CANCELLED;
        if (paid == null) paid = BigDecimal.ZERO;
        if (total == null) total = BigDecimal.ZERO;

        if (total.compareTo(BigDecimal.ZERO) <= 0) return InvoiceStatus.UNPAID;
        if (paid.compareTo(BigDecimal.ZERO) <= 0) return InvoiceStatus.UNPAID;
        if (paid.compareTo(total) >= 0) return InvoiceStatus.PAID;
        return InvoiceStatus.PARTIALLY_PAID;
    }
}