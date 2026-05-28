package com.maintainance.service_center.department;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "department")
@EntityListeners(AuditingEntityListener.class)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Column(name = "name_ar", nullable = false, length = 200)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "department_categories",
        joinColumns = @JoinColumn(name = "department_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private List<ServiceCategory> categories = new ArrayList<>();

    // Spec 022: at most one diagnostic department per center, enforced by a partial unique
    // index created from DepartmentSeeder.
    // columnDefinition includes the DB-level DEFAULT so Hibernate's ALTER TABLE on an
    // existing non-empty table can backfill historical rows without violating NOT NULL.
    @Column(name = "is_diagnostic", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    @Builder.Default
    private Boolean isDiagnostic = false;

    // Spec 022: KD with 3-decimal precision. Nullable until isDiagnostic=true.
    // Service layer rejects setting this on a non-diagnostic department.
    @Column(name = "diagnostic_fee_amount", precision = 11, scale = 3)
    private BigDecimal diagnosticFeeAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;

    public List<Long> getCategoryIds() {
        return categories.stream().map(ServiceCategory::getId).toList();
    }
}
