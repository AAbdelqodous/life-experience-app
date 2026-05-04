package com.maintainance.service_center.lookup;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "sys_lookup_detail",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lookup_id", "code"})
)
@EntityListeners(AuditingEntityListener.class)
public class LookupDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lookup_id", nullable = false)
    private Lookup lookup;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(name = "name_en", nullable = false, length = 500)
    private String nameEn;

    @Column(name = "name_ar", nullable = false, length = 500)
    private String nameAr;

    @Column(name = "short_name", length = 200)
    private String shortName;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LookupDetail parent;

    @Column(name = "extra_data", columnDefinition = "text")
    private String extraData;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 200)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "updated_by", length = 200)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
