package com.maintainance.service_center.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {
    private Long id;
    private String complaintNumber;
    private ComplaintType type;
    private String subject;
    private String description;
    private ComplaintStatus status;
    private ComplaintPriority priority;
    private String resolution;
    private Long centerId;
    private String centerNameAr;
    private String centerNameEn;
    private Long bookingId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
