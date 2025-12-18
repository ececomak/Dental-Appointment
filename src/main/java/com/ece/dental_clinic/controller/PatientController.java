package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.*;
import com.ece.dental_clinic.enums.*;
import com.ece.dental_clinic.repository.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PatientController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final TreatmentRepository treatmentRepository;
    private final AppointmentTreatmentRepository appointmentTreatmentRepository;
    private final InvoiceRepository invoiceRepository;

    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);

    public PatientController(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DentistRepository dentistRepository,
            TreatmentRepository treatmentRepository,
            AppointmentTreatmentRepository appointmentTreatmentRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.dentistRepository = dentistRepository;
        this.treatmentRepository = treatmentRepository;
        this.appointmentTreatmentRepository = appointmentTreatmentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/patient/home")
    public String patientHome(
            Authentication authentication,
            Model model,
            @RequestParam(value = "hidePast", required = false, defaultValue = "true") boolean hidePast,
            @RequestParam(value = "days", required = false, defaultValue = "30") int days,
            @RequestParam(value = "status", required = false) String statusRaw,
            @RequestParam(value = "dentistId", required = false) Long dentistId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page
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

        Pageable pageable = PageRequest.of(Math.max(page, 0), 10); // 10 satır/sayfa
        Page<Appointment> apPage = appointmentRepository.pagePatientAppointments(
                email, fromDate, null, status, dentistId, pageable
        );

        List<Appointment> appointments = apPage.getContent();
        model.addAttribute("apPage", apPage);
        model.addAttribute("appointments", appointments);

        Map<Long, String> treatmentByAppointmentId = new HashMap<>();
        List<Long> appointmentIds = appointments.stream()
                .map(Appointment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!appointmentIds.isEmpty()) {
            List<AppointmentTreatment> ats = appointmentTreatmentRepository.findByAppointment_IdIn(appointmentIds);

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

        Map<Long, Invoice> invoiceByAppointmentId = new HashMap<>();
        for (Long appointmentId : appointmentIds) {
            invoiceRepository.findByAppointment_Id(appointmentId)
                    .ifPresent(inv -> invoiceByAppointmentId.put(appointmentId, inv));
        }

        model.addAttribute("treatmentByAppointmentId", treatmentByAppointmentId);
        model.addAttribute("invoiceByAppointmentId", invoiceByAppointmentId);

        model.addAttribute("hidePast", hidePast);
        model.addAttribute("days", days);
        model.addAttribute("statusRaw", statusRaw);
        model.addAttribute("dentistId", dentistId);
        model.addAttribute("dentists", dentistRepository.findAll());
        model.addAttribute("statuses", AppointmentStatus.values());

        return "patient-home";
    }

    @PostMapping("/patient/appointments/{id}/archive")
    public String archiveAppointmentAsPatient(@PathVariable Long id, Authentication authentication) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();
        if (a.getPatient() == null || a.getPatient().getUserAccount() == null
                || !email.equals(a.getPatient().getUserAccount().getEmail())) {
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
                throw new RuntimeException("Ödenmemiş/eksik ödenmiş fatura varken arşivlenemez.");
            }
        }

        a.setArchivedAt(LocalDateTime.now());
        appointmentRepository.save(a);

        return "redirect:/patient/home";
    }

    @GetMapping("/patient/appointments/new")
    public String newAppointmentForm(
            @RequestParam(value = "dentistId", required = false) Long dentistId,
            @RequestParam(value = "treatmentId", required = false) Long treatmentId,
            @RequestParam(value = "date", required = false) String dateStr,
            Model model
    ) {
        model.addAttribute("dentists", dentistRepository.findAll());
        model.addAttribute("treatments", treatmentRepository.findByActiveTrueOrderByNameAsc());

        model.addAttribute("selectedDentistId", dentistId);
        model.addAttribute("selectedTreatmentId", treatmentId);
        model.addAttribute("selectedDate", dateStr);

        Integer slotMinutes = null;

        if (treatmentId != null) {
            Treatment selected = treatmentRepository.findById(treatmentId)
                    .orElseThrow(() -> new RuntimeException("İşlem bulunamadı: " + treatmentId));

            if (selected.getDefaultDurationMinutes() != null && selected.getDefaultDurationMinutes() > 0) {
                slotMinutes = selected.getDefaultDurationMinutes();
            }
        }

        if (slotMinutes == null) slotMinutes = DEFAULT_SLOT_MINUTES;
        model.addAttribute("slotMinutes", slotMinutes);

        if (dentistId != null && treatmentId != null && dateStr != null && !dateStr.isBlank()) {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

            LocalTime lastStart = WORK_END.minusMinutes(slotMinutes);

            LocalDateTime dayStart = date.atTime(WORK_START);
            LocalDateTime dayEnd = date.atTime(WORK_END);

            List<Appointment> busy = appointmentRepository
                    .findByDentist_IdAndAppointmentDatetimeBetweenAndStatusNotInOrderByAppointmentDatetimeAsc(
                            dentistId,
                            dayStart,
                            dayEnd,
                            List.of(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.EXPIRED)
                    );

            Set<LocalDateTime> busyTimes = busy.stream()
                    .map(Appointment::getAppointmentDatetime)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<LocalDateTime> slots = new ArrayList<>();
            LocalDateTime t = dayStart;
            while (!t.toLocalTime().isAfter(lastStart)) {
                slots.add(t);
                t = t.plusMinutes(slotMinutes);
            }

            LocalDateTime now = LocalDateTime.now();
            slots = slots.stream().filter(s -> !s.isBefore(now)).collect(Collectors.toList());

            slots = slots.stream().filter(s -> !busyTimes.contains(s)).collect(Collectors.toList());

            model.addAttribute("slots", slots);
        }

        return "patient-appointment-new";
    }

    @PostMapping("/patient/appointments")
    public String createAppointment(
            Authentication authentication,
            @RequestParam("dentistId") Long dentistId,
            @RequestParam("treatmentId") Long treatmentId,
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

        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("İşlem bulunamadı: " + treatmentId));

        LocalDateTime dt = LocalDateTime.parse(appointmentDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if (dt.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Geçmiş bir tarih/saat için randevu alınamaz.");
        }

        int slotMinutes = (treatment.getDefaultDurationMinutes() != null && treatment.getDefaultDurationMinutes() > 0)
                ? treatment.getDefaultDurationMinutes()
                : DEFAULT_SLOT_MINUTES;

        LocalTime time = dt.toLocalTime();
        LocalTime lastStart = WORK_END.minusMinutes(slotMinutes);

        if (time.isBefore(WORK_START) || time.isAfter(lastStart)) {
            throw new RuntimeException("Randevu saatleri sadece 09:00 - " + lastStart + " arası seçilebilir.");
        }

        if (time.getMinute() % slotMinutes != 0) {
            throw new RuntimeException("Randevu saati " + slotMinutes + " dakikalık aralıklara uygun olmalı.");
        }

        boolean occupied = appointmentRepository.existsByDentist_IdAndAppointmentDatetimeAndStatusNotIn(
                dentistId,
                dt,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.EXPIRED)
        );

        if (occupied) {
            throw new RuntimeException("Bu doktorun bu tarih/saat için zaten randevusu var.");
        }

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setDentist(dentist);
        a.setClinic(dentist.getClinic());
        a.setAppointmentDatetime(dt);
        a.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(a);

        double unitPrice = (treatment.getDefaultPrice() != null) ? treatment.getDefaultPrice() : 0.0;
        int qty = 1;
        double totalPrice = unitPrice * qty;

        AppointmentTreatment at = new AppointmentTreatment();
        at.setAppointment(a);
        at.setTreatment(treatment);
        at.setQuantity(qty);
        at.setUnitPrice(unitPrice);
        at.setTotalPrice(totalPrice);
        appointmentTreatmentRepository.save(at);

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

        if (a.getAppointmentDatetime() == null || a.getAppointmentDatetime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Bu randevunun tarihi geçmiş.");
        }

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

    @PostMapping("/patient/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, Authentication authentication) {

        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        String email = authentication.getName();

        if (a.getPatient() == null
                || a.getPatient().getUserAccount() == null
                || !email.equals(a.getPatient().getUserAccount().getEmail())) {
            throw new RuntimeException("Bu randevu üzerinde işlem yetkin yok.");
        }

        if (a.getAppointmentDatetime() == null || a.getAppointmentDatetime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Geçmiş randevu iptal edilemez.");
        }

        if (a.getStatus() != AppointmentStatus.SCHEDULED
                && a.getStatus() != AppointmentStatus.PATIENT_CONFIRMED) {
            throw new RuntimeException("Bu randevu bu durumda iptal edilemez: " + a.getStatus());
        }

        a.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(a);

        return "redirect:/patient/home";
    }
}