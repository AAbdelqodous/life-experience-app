package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "booking_work_progress")
public class BookingWorkProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private WorkStage stage;
    
    @Column(length = 500)
    private String notes;
    
    @Column(length = 500)
    private String notesAr;
    
    @Column(length = 500)
    private String internalNotes;
    
    private Integer estimatedMinutesRemaining;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(length = 255, nullable = false)
    private String createdByName;
    
    @OneToMany(mappedBy = "progress", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingMedia> media = new ArrayList<>();
}