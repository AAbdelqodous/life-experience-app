package com.maintainance.service_center.fulfillment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Spec 008 — a center's authored fulfillment capability: which modes it offers, the governorates it
 * serves, and the pickup/at-home fee rules. One row per center (unique center_id). When absent, the
 * service falls back to a sensible platform default, so this is purely additive — existing centers
 * keep working until an owner customizes their capability.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "fulfillment_capability")
@EntityListeners(AuditingEntityListener.class)
public class FulfillmentCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_id", unique = true, nullable = false)
    private Long centerId;

    /** Subset of FulfillmentMode names the center offers. DROP_OFF is always implicitly available. */
    @ElementCollection
    @CollectionTable(name = "fulfillment_capability_modes", joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "mode")
    @Builder.Default
    private List<String> supportedModes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "fulfillment_capability_areas", joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "governorate")
    @Builder.Default
    private List<String> serviceAreaGovernorates = new ArrayList<>();

    /** PICKUP_DELIVERY fee = base + perKm × distance. AT_HOME fee = flat. KD, 3 decimals. */
    @Column(name = "pickup_base", precision = 10, scale = 3)
    private BigDecimal pickupBase;

    @Column(name = "pickup_per_km", precision = 10, scale = 3)
    private BigDecimal pickupPerKm;

    @Column(name = "at_home_flat", precision = 10, scale = 3)
    private BigDecimal atHomeFlat;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
