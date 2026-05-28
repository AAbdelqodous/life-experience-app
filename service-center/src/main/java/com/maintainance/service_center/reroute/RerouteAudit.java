package com.maintainance.service_center.reroute;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Append-only audit record of every re-route operation (spec 022 FR-DR-023 / FR-DR-024).
 * <p>No setter exposed for {@code createdAt}; no {@code @LastModifiedDate}; the repository
 * exposes only {@code save} + read methods. Once written, the row never changes.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "reroute_audit",
    indexes = {
        @Index(name = "idx_reroute_booking", columnList = "booking_id"),
        @Index(name = "idx_reroute_from_dept_created", columnList = "from_department_id,created_at"),
        @Index(name = "idx_reroute_to_dept_created", columnList = "to_department_id,created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class RerouteAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_department_id", nullable = false)
    private Department fromDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_department_id", nullable = false)
    private Department toDepartment;

    // Null when the booking was already unassigned at re-route time
    // (e.g., owner re-routes a queued unclaimed booking).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_membership_id")
    private CenterMembership fromMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id", nullable = false)
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private RerouteReason reason;

    @Column(name = "note", length = 500)
    private String note;

    // True when this is the first re-route on a booking that originated in a diagnostic
    // department — the "diagnostic classification" event (FR-DR-023). Computed at write time
    // by RerouteService.
    @Column(name = "is_initial_diagnostic_classification", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    @Builder.Default
    private Boolean isInitialDiagnosticClassification = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
