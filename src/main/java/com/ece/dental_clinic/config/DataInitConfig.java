package com.ece.dental_clinic.config;

import com.ece.dental_clinic.entity.UserAccount;
import com.ece.dental_clinic.enums.UserRole;
import com.ece.dental_clinic.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitConfig {

    @Bean
    public CommandLineRunner initData(UserAccountRepository userAccountRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            long count = userAccountRepository.count();
            System.out.println(">> User count: " + count);

            if (count == 0) {
                UserAccount user = new UserAccount();
                user.setEmail("test@clinic.com");
                user.setPasswordHash(passwordEncoder.encode("123456"));
                user.setRole(UserRole.PATIENT);
                user.setActive(true);

                userAccountRepository.save(user);
                System.out.println(">> Test user inserted");
            }
        };
    }
}