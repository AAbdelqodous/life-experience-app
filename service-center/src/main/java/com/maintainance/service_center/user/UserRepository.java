package com.maintainance.service_center.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Page<User> findByUserTypeAndApprovalStatus(UserType userType, ApprovalStatus approvalStatus, Pageable pageable);
}
