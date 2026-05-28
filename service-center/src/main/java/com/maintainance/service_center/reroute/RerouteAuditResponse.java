package com.maintainance.service_center.reroute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wire shape for a single re-route audit row (spec 022 §Endpoint 3 Response).
 * Mirrors the frontend's {@code types/reroute.ts RerouteAudit} interface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerouteAuditResponse {
    private Long id;
    private Long bookingId;
    private Long fromDepartmentId;
    private String fromDepartmentNameAr;
    private String fromDepartmentNameEn;
    private Long toDepartmentId;
    private String toDepartmentNameAr;
    private String toDepartmentNameEn;
    private Long fromMembershipId;
    private String fromMembershipDisplayName;
    private Long triggeredByUserId;
    private String triggeredByUserDisplayName;
    private RerouteReason reason;
    private String note;
    private Boolean isInitialDiagnosticClassification;
    private LocalDateTime createdAt;

    public static RerouteAuditResponse from(RerouteAudit a) {
        var fromMembership = a.getFromMembership();
        var triggeredBy = a.getTriggeredBy();
        return RerouteAuditResponse.builder()
                .id(a.getId())
                .bookingId(a.getBooking() != null ? a.getBooking().getId() : null)
                .fromDepartmentId(a.getFromDepartment() != null ? a.getFromDepartment().getId() : null)
                .fromDepartmentNameAr(a.getFromDepartment() != null ? a.getFromDepartment().getNameAr() : null)
                .fromDepartmentNameEn(a.getFromDepartment() != null ? a.getFromDepartment().getNameEn() : null)
                .toDepartmentId(a.getToDepartment() != null ? a.getToDepartment().getId() : null)
                .toDepartmentNameAr(a.getToDepartment() != null ? a.getToDepartment().getNameAr() : null)
                .toDepartmentNameEn(a.getToDepartment() != null ? a.getToDepartment().getNameEn() : null)
                .fromMembershipId(fromMembership != null ? fromMembership.getId() : null)
                .fromMembershipDisplayName(fromMembership != null ? membershipDisplayName(fromMembership) : null)
                .triggeredByUserId(triggeredBy != null ? triggeredBy.getId().longValue() : null)
                .triggeredByUserDisplayName(triggeredBy != null ? triggeredBy.fullName() : null)
                .reason(a.getReason())
                .note(a.getNote())
                .isInitialDiagnosticClassification(Boolean.TRUE.equals(a.getIsInitialDiagnosticClassification()))
                .createdAt(a.getCreatedAt())
                .build();
    }

    private static String membershipDisplayName(com.maintainance.service_center.staff.CenterMembership m) {
        if (m.getUserFirstname() != null || m.getUserLastname() != null) {
            return (m.getUserFirstname() == null ? "" : m.getUserFirstname())
                    + (m.getUserLastname() == null ? "" : " " + m.getUserLastname()).trim();
        }
        return m.getUser() != null ? m.getUser().fullName() : null;
    }
}
