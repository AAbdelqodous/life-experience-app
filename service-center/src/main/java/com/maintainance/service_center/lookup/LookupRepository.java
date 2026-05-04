package com.maintainance.service_center.lookup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Long> {

    Optional<Lookup> findByCode(String code);

    boolean existsByCode(String code);

    List<Lookup> findByIsActiveTrueOrderByCode();

    @Query("SELECT l FROM Lookup l WHERE l.code IN :codes AND l.isActive = true")
    List<Lookup> findByCodes(@Param("codes") List<String> codes);
}
