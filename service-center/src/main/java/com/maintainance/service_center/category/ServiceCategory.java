package com.maintainance.service_center.category;

import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.service.Service;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "service_categories")
@EntityListeners(AuditingEntityListener.class)
public class ServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nameAr;

    @Column(nullable = false)
    private String nameEn;

    private String descriptionAr;
    private String descriptionEn;

    private String iconUrl;

    private Integer displayOrder = 0;

    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ServiceCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<ServiceCategory> subcategories;

    @ManyToMany(mappedBy = "categories")
    private List<MaintenanceCenter> centers;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "category_services",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<Service> services = new HashSet<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "category_allowed_service_types",
            joinColumns = @JoinColumn(name = "category_id")
    )
    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    private Set<ServiceType> allowedServiceTypes = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_by", insertable = false)
    private LocalDateTime updatedAt;
}
