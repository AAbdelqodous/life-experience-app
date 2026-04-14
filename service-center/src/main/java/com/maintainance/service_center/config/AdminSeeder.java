package com.maintainance.service_center.config;

import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminRole();
        seedAdminUser();
    }

    private void seedAdminRole() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
            log.info("Seeded ROLE_ADMIN");
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin@experience.com";
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found after seeding"));

        User admin = User.builder()
                .firstname("Admin")
                .lastname("System")
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin@123"))
                .enabled(true)
                .accountLocked(false)
                .userType(UserType.CUSTOMER)
                .approvalStatus(ApprovalStatus.APPROVED)
                .roles(List.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Seeded admin user: {} / Admin@123", adminEmail);
    }
}
