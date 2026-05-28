package com.maintainance.service_center.staff;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "center_memberships")
@EntityListeners(AuditingEntityListener.class)
public class CenterMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Cached at invite-accept time so historical attribution (FR-011) survives
    // the underlying User row being hard-deleted.
    @Column(name = "user_firstname")
    private String userFirstname;

    @Column(name = "user_lastname")
    private String userLastname;

    @Column(name = "user_email")
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CenterRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "department_memberships",
        joinColumns = @JoinColumn(name = "membership_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private List<Department> departments = new ArrayList<>();

    public List<Long> getDepartmentIds() {
        return departments.stream().map(Department::getId).toList();
    }

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
