package com.maintainance.service_center.trust;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CenterTrustBadgeRepository extends JpaRepository<CenterTrustBadge, Long> {

    List<CenterTrustBadge> findByCenterId(Long centerId);

    boolean existsByCenterIdAndBadgeType(Long centerId, TrustBadgeType badgeType);

    void deleteByCenterIdAndBadgeType(Long centerId, TrustBadgeType badgeType);
}
