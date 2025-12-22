package com.ece.dental_clinic.controller;

import com.ece.dental_clinic.entity.Clinic;
import com.ece.dental_clinic.entity.Dentist;
import com.ece.dental_clinic.entity.Patient;
import com.ece.dental_clinic.entity.UserAccount;
import com.ece.dental_clinic.enums.UserRole;
import com.ece.dental_clinic.repository.ClinicRepository;
import com.ece.dental_clinic.repository.DentistRepository;
import com.ece.dental_clinic.repository.PatientRepository;
import com.ece.dental_clinic.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Controller
public class RegistrationController {

    private final UserAccountRepository userAccountRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final ClinicRepository clinicRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserAccountRepository userAccountRepository,
                                  PatientRepository patientRepository,
                                  DentistRepository dentistRepository,
                                  ClinicRepository clinicRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.patientRepository = patientRepository;
        this.dentistRepository = dentistRepository;
        this.clinicRepository = clinicRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterForm(
            @RequestParam(value = "role", required = false) String roleRaw,
            @RequestParam(value = "err", required = false) String err,
            Model model
    ) {
        UserRole selectedRole = UserRole.PATIENT;
        if (roleRaw != null && !roleRaw.isBlank()) {
            try {
                selectedRole = UserRole.valueOf(roleRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        model.addAttribute("selectedRole", selectedRole);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("clinics", clinicRepository.findAll());
        model.addAttribute("err", err);

        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String passwordConfirm,
            @RequestParam("role") String roleRaw,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "birthDate", required = false) String birthDateStr,
            @RequestParam(value = "clinicId", required = false) Long clinicId,
            @RequestParam(value = "specialty", required = false) String specialty
    ) {

        if (email == null || email.isBlank()
                || password == null || password.isBlank()
                || passwordConfirm == null || passwordConfirm.isBlank()
                || firstName == null || firstName.isBlank()
                || lastName == null || lastName.isBlank()) {

            return "redirect:/register?err=missing&role=" + safeRole(roleRaw);
        }

        if (!password.equals(passwordConfirm)) {
            return "redirect:/register?err=pwd&role=" + safeRole(roleRaw);
        }

        Optional<UserAccount> existing = userAccountRepository.findByEmail(email.trim());
        if (existing.isPresent()) {
            return "redirect:/register?err=email&role=" + safeRole(roleRaw);
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleRaw.trim().toUpperCase());
        } catch (Exception e) {
            role = UserRole.PATIENT;
        }

        UserAccount ua = new UserAccount();
        ua.setEmail(email.trim());
        ua.setPasswordHash(passwordEncoder.encode(password));
        ua.setRole(role);
        ua.setActive(true);
        userAccountRepository.save(ua);

        if (role == UserRole.PATIENT) {

            Patient p = new Patient();
            p.setUserAccount(ua);
            p.setFirstName(firstName.trim());
            p.setLastName(lastName.trim());
            p.setPhone(phone);
            p.setAddress(address);

            if (birthDateStr != null && !birthDateStr.isBlank()) {
                try {
                    p.setBirthDate(LocalDate.parse(birthDateStr.trim()));
                } catch (DateTimeParseException ignored) {
                }
            }

            patientRepository.save(p);

        } else if (role == UserRole.DENTIST) {

            Clinic clinic = null;
            if (clinicId != null) {
                clinic = clinicRepository.findById(clinicId).orElse(null);
            }
            if (clinic == null) {
                clinic = clinicRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Kayıt için klinik bulunamadı."));
            }

            Dentist d = new Dentist();
            d.setUserAccount(ua);
            d.setClinic(clinic);
            d.setFirstName(firstName.trim());
            d.setLastName(lastName.trim());
            d.setPhone(phone);
            d.setSpecialty(specialty);

            dentistRepository.save(d);
        }

        return "redirect:/login?registered";
    }

    private String safeRole(String roleRaw) {
        if (roleRaw == null || roleRaw.isBlank()) {
            return "PATIENT";
        }
        return roleRaw.trim().toUpperCase();
    }
}