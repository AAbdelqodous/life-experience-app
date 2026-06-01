package com.maintainance.service_center.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepositConfigRepository extends JpaRepository<DepositConfig, Long> {
    Optional<DepositConfig> findByCenterId(Long centerId);
}
