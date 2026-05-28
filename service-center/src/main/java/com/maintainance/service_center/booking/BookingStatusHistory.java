package com.maintainance.service_center.booking;

import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "booking_status_history",
    indexes = {
        @Index(name = "idx_bsh_booking", columnList = "booking_id")
    }
)
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acting_user_id", nullable = false)
    private User actingUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "acting_role", nullable = false, length = 30)
    private CenterRole actingRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private BookingStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private BookingStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    @PrePersist
    void prePersist() {
        if (actedAt == null) {
            actedAt = LocalDateTime.now();
        }
    }
}
