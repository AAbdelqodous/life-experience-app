package com.maintainance.service_center.reroute;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.department.DepartmentRepository;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec 022 — re-route core flow + the 6 validations from RerouteService.
 * Sets up: a center with diagnostic + two working depts, an owner, an assigned technician,
 * and an unrelated technician. Each test creates its own booking with a unique state.
 */
@SpringBootTest
@ActiveProfiles("dev")
class RerouteIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired RerouteAuditRepository auditRepository;
    @Autowired RerouteService rerouteService;
    @Autowired TransactionTemplate transactionTemplate;

    private MaintenanceCenter center;
    private Department diagnosticDept;
    private Department engineDept;
    private Department electricalDept;
    private User owner;
    private User assignedTech;
    private User strangerTech;
    private CenterMembership assignedTechMembership;
    private User customer;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            Role staffRole = roleRepository.findByName("ROLE_STAFF")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STAFF").build()));

            owner = userRepository.save(User.builder()
                    .firstname("Rr").lastname("Owner")
                    .email("rr-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER)
                    .roles(List.of(ownerRole))
                    .build());

            customer = userRepository.save(User.builder()
                    .firstname("Rr").lastname("Customer")
                    .email("rr-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER)
                    .build());

            assignedTech = userRepository.save(User.builder()
                    .firstname("Rr").lastname("Tech")
                    .email("rr-tech-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.STAFF)
                    .roles(List.of(staffRole))
                    .build());

            strangerTech = userRepository.save(User.builder()
                    .firstname("Rr").lastname("Stranger")
                    .email("rr-str-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.STAFF)
                    .roles(List.of(staffRole))
                    .build());

            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز ت " + suffix).nameEn("Rr Center " + suffix)
                    .email("rr-c-" + suffix + "@test.local")
                    .phone("+96566666" + suffix.substring(0, 3))
                    .categories(new ArrayList<>())
                    .workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>())
                    .imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>())
                    .owner(owner)
                    .isVerified(false).isActive(true).enabled(true)
                    .build());

            membershipRepository.save(CenterMembership.builder()
                    .user(owner).center(center)
                    .role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now()).build());

            diagnosticDept = departmentRepository.save(Department.builder()
                    .center(center)
                    .nameAr("تشخيص " + suffix).nameEn("Diagnostic " + suffix)
                    .displayOrder(0).isActive(true)
                    .categories(new ArrayList<>())
                    .isDiagnostic(true)
                    .diagnosticFeeAmount(new BigDecimal("5.000"))
                    .build());

            engineDept = departmentRepository.save(Department.builder()
                    .center(center)
                    .nameAr("محرك " + suffix).nameEn("Engine " + suffix)
                    .displayOrder(1).isActive(true)
                    .categories(new ArrayList<>())
                    .build());

            electricalDept = departmentRepository.save(Department.builder()
                    .center(center)
                    .nameAr("كهرباء " + suffix).nameEn("Electrical " + suffix)
                    .displayOrder(2).isActive(true)
                    .categories(new ArrayList<>())
                    .build());

            assignedTechMembership = membershipRepository.save(CenterMembership.builder()
                    .user(assignedTech).center(center)
                    .role(CenterRole.TECHNICIAN).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now())
                    .departments(new ArrayList<>(List.of(engineDept)))
                    .build());

            // Stranger tech is at the same center but a different department.
            membershipRepository.save(CenterMembership.builder()
                    .user(strangerTech).center(center)
                    .role(CenterRole.TECHNICIAN).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now())
                    .departments(new ArrayList<>(List.of(electricalDept)))
                    .build());
        });
    }

    @Test
    void assignedTechReroutes_succeeds_writesAuditUnassigns() {
        Long bookingId = createBooking(engineDept, assignedTechMembership, BookingStatus.CONFIRMED, false);

        RerouteRequest req = new RerouteRequest(electricalDept.getId(),
                RerouteReason.WRONG_DIAGNOSIS, "Compression normal");
        RerouteResponse resp = rerouteService.reroute(bookingId, req, assignedTech);

        assertThat(resp.getAudit().getFromDepartmentId()).isEqualTo(engineDept.getId());
        assertThat(resp.getAudit().getToDepartmentId()).isEqualTo(electricalDept.getId());
        assertThat(resp.getAudit().getReason()).isEqualTo(RerouteReason.WRONG_DIAGNOSIS);
        assertThat(resp.getAudit().getIsInitialDiagnosticClassification()).isFalse();
        assertThat(resp.getUpdatedBooking().getDepartmentId()).isEqualTo(electricalDept.getId());
        assertThat(resp.getUpdatedBooking().getAssignedMembershipId()).isNull();

        // Booking row reflects the same state.
        Booking after = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(after.getDepartment().getId()).isEqualTo(electricalDept.getId());
        assertThat(after.getAssignedMembership()).isNull();
        assertThat(auditRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).hasSize(1);
    }

    @Test
    void ownerReroutes_anyBooking_succeeds() {
        // Booking is assigned to a different tech; owner overrides.
        Long bookingId = createBooking(engineDept, assignedTechMembership, BookingStatus.CONFIRMED, false);

        RerouteResponse resp = rerouteService.reroute(bookingId,
                new RerouteRequest(electricalDept.getId(), RerouteReason.STAFF_UNAVAILABLE, "Engine overloaded"),
                owner);

        assertThat(resp.getAudit().getTriggeredByUserId()).isEqualTo(owner.getId().longValue());
        assertThat(resp.getUpdatedBooking().getDepartmentId()).isEqualTo(electricalDept.getId());
    }

    @Test
    void unrelatedTechnician_cannotReroute() {
        Long bookingId = createBooking(engineDept, assignedTechMembership, BookingStatus.CONFIRMED, false);

        assertThatThrownBy(() -> rerouteService.reroute(bookingId,
                new RerouteRequest(electricalDept.getId(), RerouteReason.OTHER, null),
                strangerTech))
                .isInstanceOfSatisfying(RerouteException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(BusinessErrorCodes.FORBIDDEN_REROUTE));
    }

    @Test
    void rerouteIntoDiagnostic_rejected() {
        Long bookingId = createBooking(engineDept, assignedTechMembership, BookingStatus.CONFIRMED, false);
        assertThatThrownBy(() -> rerouteService.reroute(bookingId,
                new RerouteRequest(diagnosticDept.getId(), RerouteReason.OTHER, null),
                owner))
                .isInstanceOfSatisfying(RerouteException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(BusinessErrorCodes.CANNOT_REROUTE_INTO_DIAGNOSTIC));
    }

    @Test
    void noOpReroute_rejected() {
        Long bookingId = createBooking(engineDept, assignedTechMembership, BookingStatus.CONFIRMED, false);
        assertThatThrownBy(() -> rerouteService.reroute(bookingId,
                new RerouteRequest(engineDept.getId(), RerouteReason.OTHER, null),
                owner))
                .isInstanceOfSatisfying(RerouteException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(BusinessErrorCodes.NO_OP_REROUTE));
    }

    @Test
    void terminalStatus_rejected() {
        Long bookingId = createBooking(engineDept, assignedTechMembership, BookingStatus.COMPLETED, false);
        assertThatThrownBy(() -> rerouteService.reroute(bookingId,
                new RerouteRequest(electricalDept.getId(), RerouteReason.OTHER, null),
                owner))
                .isInstanceOfSatisfying(RerouteException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(BusinessErrorCodes.INVALID_BOOKING_STATUS_FOR_REROUTE));
    }

    @Test
    void rerouteFromDiagnostic_setsInitialClassificationFlag() {
        Long bookingId = createBooking(diagnosticDept, null, BookingStatus.CONFIRMED, true);

        RerouteResponse resp = rerouteService.reroute(bookingId,
                new RerouteRequest(electricalDept.getId(),
                        RerouteReason.SPECIALIST_NEEDED, "Alternator likely failing"),
                owner);

        assertThat(resp.getAudit().getIsInitialDiagnosticClassification()).isTrue();

        // A second re-route on the same booking is NOT initial classification.
        RerouteResponse second = rerouteService.reroute(bookingId,
                new RerouteRequest(engineDept.getId(), RerouteReason.WRONG_DIAGNOSIS, null),
                owner);
        assertThat(second.getAudit().getIsInitialDiagnosticClassification()).isFalse();
    }

    @Test
    void rerouteHistory_returnsAllEvents_chronologically() {
        Long bookingId = createBooking(diagnosticDept, null, BookingStatus.CONFIRMED, true);

        rerouteService.reroute(bookingId,
                new RerouteRequest(electricalDept.getId(), RerouteReason.SPECIALIST_NEEDED, "first"),
                owner);
        rerouteService.reroute(bookingId,
                new RerouteRequest(engineDept.getId(), RerouteReason.WRONG_DIAGNOSIS, "second"),
                owner);

        List<RerouteAuditResponse> history = rerouteService.getHistory(bookingId, owner);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getIsInitialDiagnosticClassification()).isTrue();
        assertThat(history.get(0).getNote()).isEqualTo("first");
        assertThat(history.get(1).getIsInitialDiagnosticClassification()).isFalse();
        assertThat(history.get(1).getNote()).isEqualTo("second");
    }

    private Long createBooking(Department dept, CenterMembership assigned,
                               BookingStatus status, boolean passedThroughDiagnostic) {
        return transactionTemplate.execute(s -> {
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("RR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(10, 0))
                    .bookingStatus(status)
                    .department(dept)
                    .assignedMembership(assigned)
                    .passedThroughDiagnostic(passedThroughDiagnostic)
                    .build());
            return b.getId();
        });
    }
}
