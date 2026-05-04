package com.maintainance.service_center.lookup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LookupDetailRepository extends JpaRepository<LookupDetail, Long> {

    List<LookupDetail> findByLookupCodeAndIsActiveTrueOrderBySortOrderAscIdAsc(String lookupCode);

    List<LookupDetail> findByLookupCodeOrderBySortOrderAscIdAsc(String lookupCode);

    Optional<LookupDetail> findByLookupCodeAndCode(String lookupCode, String code);

    boolean existsByLookupCodeAndCode(String lookupCode, String code);
}
