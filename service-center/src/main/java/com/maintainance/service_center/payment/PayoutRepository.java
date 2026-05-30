package com.maintainance.service_center.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findByCenterIdOrderByRequestedAtDesc(Long centerId);
}
