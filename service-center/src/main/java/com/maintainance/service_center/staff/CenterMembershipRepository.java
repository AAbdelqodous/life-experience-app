package com.maintainance.service_center.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterMembershipRepository extends JpaRepository<CenterMembership, Long> {

    Page<CenterMembership> findByCenterId(Long centerId, Pageable pageable);

    Page<CenterMembership> findByCenterIdAndStatus(Long centerId, MembershipStatus status, Pageable pageable);

    Optional<CenterMembership> findByCenterIdAndUserId(Long centerId, Integer userId);

    Optional<CenterMembership> findByCenterIdAndUserIdAndStatus(Long centerId, Integer userId, MembershipStatus status);

    List<CenterMembership> findByUserId(Integer userId);

    @Query("SELECT m FROM CenterMembership m WHERE m.user.id = :userId AND m.status = :status")
    List<CenterMembership> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") MembershipStatus status);

    @Query("SELECT m FROM CenterMembership m WHERE m.center.id = :centerId AND m.role IN :roles")
    List<CenterMembership> findByCenterIdAndRoleIn(@Param("centerId") Long centerId, @Param("roles") List<CenterRole> roles);

    @Query("SELECT m FROM CenterMembership m WHERE m.center.id = :centerId AND m.role IN :roles AND m.status = :status")
    List<CenterMembership> findByCenterIdAndRoleInAndStatus(@Param("centerId") Long centerId, 
                                                             @Param("roles") List<CenterRole> roles, 
                                                             @Param("status") MembershipStatus status);

    boolean existsByCenterIdAndUserId(Long centerId, Integer userId);
}
