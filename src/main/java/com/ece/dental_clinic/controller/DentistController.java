package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.Appointment;
import com.ece.dental_clinic.entity.AppointmentTreatment;
import com.ece.dental_clinic.enums.AppointmentStatus;
import com.ece.dental_clinic.repository.AppointmentRepository;
import com.ece.dental_clinic.repository.AppointmentTreatmentRepository;
import com.ece.dental_clinic.repository.InvoiceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public String dentistHome(Authentication authentication, Model model) {

        appointmentRepository.expirePastAppointments(
                LocalDateTime.now(),
                AppointmentStatus.EXPIRED,
                List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.EXPIRED)
        );

        String email = authentication.getName();
        List<Appointment> appointments =
                appointmentRepository.findByDentist_UserAccount_EmailOrderByAppointmentDatetimeDesc(email);

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

        return "dentist-home";
    }

    @PostMapping("/dentist/appointments/{id}/confirm")
    public String confirmAppointment(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getDentist() == null
                || a.getDentist().getUserAccount() == null
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
        if (a.getDentist() == null
                || a.getDentist().getUserAccount() == null
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
        if (a.getDentist() == null
                || a.getDentist().getUserAccount() == null
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
}