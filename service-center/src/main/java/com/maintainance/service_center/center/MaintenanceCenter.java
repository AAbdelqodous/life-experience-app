package com.maintainance.service_center.center;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "maintenance_centers")
@EntityListeners(AuditingEntityListener.class)
public class MaintenanceCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nameAr;

    @Column(nullable = false)
    private String nameEn;

    @Column(length = 1000)
    private String descriptionAr;

    @Column(length = 1000)
    private String descriptionEn;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String alternativePhone;

    @Embedded
    private Address address;

    private Double latitude;

    private Double longitude;

    private LocalTime openingTime;

    private LocalTime closingTime;


}
