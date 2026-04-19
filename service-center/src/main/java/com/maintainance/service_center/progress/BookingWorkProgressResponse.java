package com.maintainance.service_center.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWorkProgressResponse {
    
    private Long id;
    private String stage;
    private String notes;
    private String notesAr;
    private String internalNotes;
    private Integer estimatedMinutesRemaining;
    private List<BookingMediaResponse> photos;
    private LocalDateTime createdAt;
    private String createdByName;
}