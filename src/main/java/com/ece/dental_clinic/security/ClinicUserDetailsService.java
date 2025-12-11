package com.ece.dental_clinic.security;

import com.ece.dental_clinic.entity.UserAccount;
import com.ece.dental_clinic.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ClinicUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public ClinicUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username = email
        UserAccount user = userAccountRepository
                .findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

        return new ClinicUserDetails(user);
    }
}