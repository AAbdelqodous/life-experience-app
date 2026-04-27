package com.maintainance.service_center.progress;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkProgressRequest {
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
    
    @Size(max = 500, message = "Notes (Arabic) must not exceed 500 characters")
    private String notesAr;
    
    @Size(max = 500, message = "Internal notes must not exceed 500 characters")
    private String internalNotes;
    
    private Integer estimatedMinutesRemaining;
}