package com.ece.dental_clinic.config;

import com.ece.dental_clinic.entity.UserAccount;
import com.ece.dental_clinic.enums.UserRole;
import com.ece.dental_clinic.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitConfig {

    @Bean
    public CommandLineRunner initData(UserAccountRepository userAccountRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {

            String patientEmail = "test@clinic.com";
            if (userAccountRepository.findByEmail(patientEmail).isEmpty()) {
                UserAccount patient = new UserAccount();
                patient.setEmail(patientEmail);
                patient.setPasswordHash(passwordEncoder.encode("123456"));
                patient.setRole(UserRole.PATIENT);
                patient.setActive(true);
                patient.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(patient);
            }

            String dentistEmail = "dentist@clinic.com";
            if (userAccountRepository.findByEmail(dentistEmail).isEmpty()) {
                UserAccount dentist = new UserAccount();
                dentist.setEmail(dentistEmail);
                dentist.setPasswordHash(passwordEncoder.encode("123456"));
                dentist.setRole(UserRole.DENTIST);
                dentist.setActive(true);
                dentist.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(dentist);
            }

            System.out.println(">> User count: " + userAccountRepository.count());
        };
    }
}
