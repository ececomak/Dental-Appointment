package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.Appointment;
import com.ece.dental_clinic.enums.AppointmentStatus;
import com.ece.dental_clinic.repository.AppointmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class DentistController {

    private final AppointmentRepository appointmentRepository;

    public DentistController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/dentist/home")
    public String dentistHome(Authentication authentication, Model model) {
        appointmentRepository.expirePastAppointments(
                LocalDateTime.now(),
                AppointmentStatus.EXPIRED,
                List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.EXPIRED)
        );

        String email = authentication.getName();
        model.addAttribute("appointments",
                appointmentRepository.findByDentist_UserAccount_EmailOrderByAppointmentDatetimeDesc(email)
        );
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

        return "redirect:/dentist/home";
    }
}