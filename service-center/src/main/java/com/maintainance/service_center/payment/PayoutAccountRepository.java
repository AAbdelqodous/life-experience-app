package com.maintainance.service_center.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PayoutAccountRepository extends JpaRepository<PayoutAccount, Long> {
    Optional<PayoutAccount> findByCenterId(Long centerId);
}
