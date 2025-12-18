package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.*;
import com.ece.dental_clinic.enums.*;
import com.ece.dental_clinic.repository.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DentistController {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentTreatmentRepository appointmentTreatmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final JdbcTemplate jdbcTemplate;

    public DentistController(
            AppointmentRepository appointmentRepository,
            AppointmentTreatmentRepository appointmentTreatmentRepository,
            InvoiceRepository invoiceRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentTreatmentRepository = appointmentTreatmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/dentist/home")
    public String dentistHome(
            Authentication authentication,
            Model model,
            @RequestParam(value = "hidePast", required = false, defaultValue = "true") boolean hidePast,
            @RequestParam(value = "days", required = false, defaultValue = "30") int days,
            @RequestParam(value = "status", required = false) String statusRaw,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "err", required = false) String err
    ) {
        appointmentRepository.expirePastAppointments(
                LocalDateTime.now(),
                AppointmentStatus.EXPIRED,
                List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.EXPIRED)
        );

        String email = authentication.getName();

        AppointmentStatus status = null;
        if (statusRaw != null && !statusRaw.isBlank()) {
            status = AppointmentStatus.valueOf(statusRaw.trim().toUpperCase());
        }

        LocalDateTime fromDate = null;
        if (hidePast) {
            fromDate = LocalDateTime.now().minusDays(Math.max(days, 1));
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), 10);
        Page<Appointment> apPage = appointmentRepository.pageDentistAppointments(
                email, fromDate, null, status, pageable
        );

        List<Appointment> appointments = apPage.getContent();
        model.addAttribute("apPage", apPage);
        model.addAttribute("appointments", appointments);

        Map<Long, String> treatmentByAppointmentId = new HashMap<>();
        List<Long> ids = appointments.stream()
                .map(Appointment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!ids.isEmpty()) {
            List<AppointmentTreatment> ats = appointmentTreatmentRepository.findByAppointment_IdIn(ids);

            Map<Long, List<AppointmentTreatment>> grouped = ats.stream()
                    .filter(at -> at.getAppointment() != null && at.getAppointment().getId() != null)
                    .collect(Collectors.groupingBy(at -> at.getAppointment().getId()));

            for (Map.Entry<Long, List<AppointmentTreatment>> e : grouped.entrySet()) {
                String names = e.getValue().stream()
                        .map(at -> at.getTreatment() != null ? at.getTreatment().getName() : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining(", "));
                treatmentByAppointmentId.put(e.getKey(), names);
            }
        }

        model.addAttribute("treatmentByAppointmentId", treatmentByAppointmentId);

        Map<Long, Long> invoiceIdByAppointmentId = new HashMap<>();
        for (Appointment a : appointments) {
            if (a.getId() == null) continue;
            invoiceRepository.findByAppointment_Id(a.getId())
                    .ifPresent(inv -> invoiceIdByAppointmentId.put(a.getId(), inv.getId()));
        }
        model.addAttribute("invoiceIdByAppointmentId", invoiceIdByAppointmentId);

        model.addAttribute("hidePast", hidePast);
        model.addAttribute("days", days);
        model.addAttribute("statusRaw", statusRaw);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("err", err);

        return "dentist-home";
    }

    @PostMapping("/dentist/appointments/{id}/confirm")
    public String confirmAppointment(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getDentist() == null || a.getDentist().getUserAccount() == null
                || !email.equals(a.getDentist().getUserAccount().getEmail())) {
            throw new RuntimeException("Bu randevu üzerinde işlem yetkin yok.");
        }

        a.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(a);
        return "redirect:/dentist/home";
    }

    @PostMapping("/dentist/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getDentist() == null || a.getDentist().getUserAccount() == null
                || !email.equals(a.getDentist().getUserAccount().getEmail())) {
            throw new RuntimeException("Bu randevu üzerinde işlem yetkin yok.");
        }

        a.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(a);
        return "redirect:/dentist/home";
    }

    @PostMapping("/dentist/appointments/{id}/complete")
    public String completeAppointment(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getDentist() == null || a.getDentist().getUserAccount() == null
                || !email.equals(a.getDentist().getUserAccount().getEmail())) {
            throw new RuntimeException("Bu randevu üzerinde işlem yetkin yok.");
        }

        a.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(a);

        if (!invoiceRepository.existsByAppointment_Id(a.getId())) {
            LocalDate due = LocalDate.now().plusDays(30);
            jdbcTemplate.update("call generate_invoice_for_appointment(?, ?)", a.getId(), due);
        }

        return "redirect:/dentist/home";
    }

    @PostMapping("/dentist/appointments/{id}/archive")
    public String archiveAppointmentAsDentist(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getDentist() == null || a.getDentist().getUserAccount() == null
                || !email.equals(a.getDentist().getUserAccount().getEmail())) {
            throw new RuntimeException("Bu randevu üzerinde işlem yetkin yok.");
        }

        if (!(a.getStatus() == AppointmentStatus.COMPLETED
                || a.getStatus() == AppointmentStatus.CANCELLED
                || a.getStatus() == AppointmentStatus.EXPIRED)) {
            throw new RuntimeException("Aktif randevu arşivlenemez.");
        }

        Optional<Invoice> invOpt = invoiceRepository.findByAppointment_Id(a.getId());
        if (invOpt.isPresent()) {
            InvoiceStatus st = invOpt.get().getStatus();
            if (st == InvoiceStatus.UNPAID || st == InvoiceStatus.PARTIALLY_PAID) {
                return "redirect:/dentist/home?err=unpaid";
            }
        }

        a.setArchivedAt(LocalDateTime.now());
        appointmentRepository.save(a);
        return "redirect:/dentist/home";
    }

    @GetMapping("/dentist/invoices/{invoiceId}")
    public String dentistViewInvoice(@PathVariable Long invoiceId, Authentication authentication, Model model) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura bulunamadı: " + invoiceId));

        String email = authentication.getName();
        boolean isOwner = inv.getAppointment() != null
                && inv.getAppointment().getDentist() != null
                && inv.getAppointment().getDentist().getUserAccount() != null
                && email.equals(inv.getAppointment().getDentist().getUserAccount().getEmail());

        if (!isOwner) throw new RuntimeException("Bu faturayı görme yetkin yok.");

        model.addAttribute("invoice", inv);
        return "dentist-invoice";
    }
}