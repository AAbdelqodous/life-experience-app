package com.maintainance.service_center.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWorkProgressResponse {
    
    private Long id;
    private Long bookingId;
    private WorkStage stage;
    private String notes;
    private String notesAr;
    private String internalNotes;
    private String photoUrl;
    private String videoUrl;
    private Integer estimatedMinutesRemaining;
    private LocalDateTime createdAt;
    private String createdByName;
}