package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "booking_media")
@EntityListeners(AuditingEntityListener.class)
public class BookingMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaCategory category;
    
    @Column(nullable = false)
    private String url;
    
    private String thumbnailUrl;
    
    @Column(length = 500)
    private String caption;
    
    @Column(length = 500)
    private String captionAr;
    
    @Column(nullable = false)
    private Boolean isVisibleToCustomer = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}