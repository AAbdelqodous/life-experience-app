package com.maintainance.service_center.trust;

import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "center_trust_badge",
        uniqueConstraints = @UniqueConstraint(columnNames = {"center_id", "badge_type"}))
@EntityListeners(AuditingEntityListener.class)
public class CenterTrustBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustBadgeType badgeType;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime earnedAt;
}
