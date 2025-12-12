package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.Appointment;
import com.ece.dental_clinic.entity.Dentist;
import com.ece.dental_clinic.entity.Patient;
import com.ece.dental_clinic.enums.AppointmentStatus;
import com.ece.dental_clinic.repository.AppointmentRepository;
import com.ece.dental_clinic.repository.DentistRepository;
import com.ece.dental_clinic.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class PatientController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;

    public PatientController(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DentistRepository dentistRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.dentistRepository = dentistRepository;
    }

    @GetMapping("/patient/home")
    public String patientHome(Authentication authentication, Model model) {
        appointmentRepository.expirePastAppointments(
                LocalDateTime.now(),
                AppointmentStatus.EXPIRED,
                List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.EXPIRED)
        );

        String email = authentication.getName();
        model.addAttribute("appointments",
                appointmentRepository.findByPatient_UserAccount_EmailOrderByAppointmentDatetimeDesc(email)
        );
        return "patient-home";
    }

    @GetMapping("/patient/appointments/new")
    public String newAppointmentForm(Model model) {
        model.addAttribute("dentists", dentistRepository.findAll());
        return "patient-appointment-new";
    }

    @PostMapping("/patient/appointments")
    public String createAppointment(
            Authentication authentication,
            @RequestParam("dentistId") Long dentistId,
            @RequestParam("appointmentDatetime") String appointmentDatetime
    ) {
        String email = authentication.getName();

        Patient patient = patientRepository.findByUserAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("Hasta bulunamadı: " + email));

        Dentist dentist = dentistRepository.findById(dentistId)
                .orElseThrow(() -> new RuntimeException("Diş Hekimi bulunamadı: " + dentistId));

        if (dentist.getClinic() == null) {
            throw new RuntimeException("Seçilen doktorun clinic bilgisi yok. dentist_id=" + dentistId);
        }

        LocalDateTime dt = LocalDateTime.parse(appointmentDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setDentist(dentist);
        a.setClinic(dentist.getClinic());
        a.setAppointmentDatetime(dt);
        a.setStatus(AppointmentStatus.SCHEDULED);

        appointmentRepository.save(a);

        return "redirect:/patient/home";
    }

    @PostMapping("/patient/appointments/{id}/confirm-attendance")
    public String confirmAttendance(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getPatient() == null
                || a.getPatient().getUserAccount() == null
                || !email.equals(a.getPatient().getUserAccount().getEmail())) {
            throw new RuntimeException("Bu randevu üzerinde işlem yetkin yok.");
        }

        if (a.getAppointmentDatetime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Bu randevunun tarihi geçmiş.");
        }

        // 24 saat kala onay
        LocalDateTime now = LocalDateTime.now();
        if (a.getAppointmentDatetime().isAfter(now.plusHours(24))) {
            throw new RuntimeException("Randevuya 24 saatten fazla var, henüz onay veremezsin.");
        }

        if (a.getStatus() == AppointmentStatus.CANCELLED
                || a.getStatus() == AppointmentStatus.EXPIRED
                || a.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Bu randevunun durumu uygun değil: " + a.getStatus());
        }

        a.setStatus(AppointmentStatus.PATIENT_CONFIRMED);
        appointmentRepository.save(a);

        return "redirect:/patient/home";
    }
}