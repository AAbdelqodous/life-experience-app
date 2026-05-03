package com.maintainance.service_center.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Page<User> findByUserTypeAndApprovalStatus(UserType userType, ApprovalStatus approvalStatus, Pageable pageable);
    Page<User> findByUserType(UserType userType, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :userType")
    long countByUserType(@Param("userType") UserType userType);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdDate >= :startDate AND u.createdDate <= :endDate")
    long countByCreatedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
