package com.maintainance.service_center.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long> {

    Optional<StaffInvitation> findByToken(String token);

    List<StaffInvitation> findByCenterId(Long centerId);

    List<StaffInvitation> findByCenterIdAndStatus(Long centerId, InvitationStatus status);

    Optional<StaffInvitation> findByCenterIdAndTargetEmailAndStatus(Long centerId, String targetEmail, InvitationStatus status);

    @Query("SELECT i FROM StaffInvitation i WHERE i.center.id = :centerId AND i.targetEmail = :targetEmail AND i.status IN :statuses")
    List<StaffInvitation> findByCenterIdAndTargetEmailAndStatusIn(@Param("centerId") Long centerId, 
                                                                   @Param("targetEmail") String targetEmail, 
                                                                   @Param("statuses") List<InvitationStatus> statuses);

    @Query("SELECT i FROM StaffInvitation i WHERE i.status = :status AND i.expiresAt < :now")
    List<StaffInvitation> findByStatusAndExpiresAtBefore(@Param("status") InvitationStatus status, 
                                                         @Param("now") LocalDateTime now);

    boolean existsByCenterIdAndTargetEmailAndStatus(Long centerId, String targetEmail, InvitationStatus status);
}
