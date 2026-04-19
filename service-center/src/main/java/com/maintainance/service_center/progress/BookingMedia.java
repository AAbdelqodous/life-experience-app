package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "booking_media")
public class BookingMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "progress_id")
    private BookingWorkProgress progress;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;
    
    @Column(length = 50, nullable = false)
    private String category;
    
    @Column(nullable = false)
    private String url;
    
    private String thumbnailUrl;
    
    @Column(length = 500)
    private String caption;
    
    @Column(length = 500)
    private String captionAr;
    
    @Column(nullable = false)
    private Boolean isVisibleToCustomer = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}