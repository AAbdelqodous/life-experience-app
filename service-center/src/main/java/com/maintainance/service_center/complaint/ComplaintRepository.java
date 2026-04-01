package com.maintainance.service_center.complaint;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByComplainantIdOrderByCreatedAtDesc(Integer complainantId, Pageable pageable);

    Page<Complaint> findByCenterIdOrderByCreatedAtDesc(Long centerId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.complainant.id = :userId AND c.status IN :statuses")
    List<Complaint> findByUserIdAndStatuses(@Param("userId") Integer userId, @Param("statuses") List<ComplaintStatus> statuses);

    @Query("SELECT c FROM Complaint c WHERE c.center.id = :centerId AND c.status IN :statuses")
    List<Complaint> findByCenterIdAndStatuses(@Param("centerId") Long centerId, @Param("statuses") List<ComplaintStatus> statuses);

    @Query("SELECT c FROM Complaint c WHERE c.booking.id = :bookingId")
    List<Complaint> findByBookingId(@Param("bookingId") Long bookingId);

    boolean existsByComplaintNumber(String complaintNumber);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.complainant.id = :userId AND c.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") ComplaintStatus status);

    Optional<Complaint> findByIdAndComplainantId(Long id, Integer complainantId);
}
