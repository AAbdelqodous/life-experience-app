package com.maintainance.service_center.booking;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.department.DepartmentRepository;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec 021 — self-claim integration tests.
 * <p>SC-SC-2: 20 simultaneous claim requests for the same booking must produce exactly one
 * success and 19 BOOKING_ALREADY_CLAIMED, with zero double-assignments.
 * <p>FR-SC-010: a technician assigned to dept A cannot claim a booking belonging to dept B
 * even via direct API call.
 * <p>Uses real Postgres so the pessimistic lock is exercised against the actual JDBC driver.
 */
@SpringBootTest
@ActiveProfiles("dev")
class SelfClaimIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingClaimAuditRepository auditRepository;
    @Autowired BookingService bookingService;
    @Autowired TransactionTemplate transactionTemplate;

    private MaintenanceCenter center;
    private Department deptA;
    private Department deptB;
    private User customer;

    @BeforeEach
    void setUp() {
        // Commit setup outside the test transaction so spawned threads see the rows.
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            Role staffRole = roleRepository.findByName("ROLE_STAFF")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STAFF").build()));

            User owner = userRepository.save(User.builder()
                    .firstname("Self-Claim").lastname("Owner")
                    .email("sc-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER)
                    .roles(List.of(ownerRole))
                    .build());

            customer = userRepository.save(User.builder()
                    .firstname("Self-Claim").lastname("Customer")
                    .email("sc-customer-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER)
                    .build());

            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز اختبار " + suffix)
                    .nameEn("Self-Claim Center " + suffix)
                    .email("sc-center-" + suffix + "@test.local")
                    .phone("+9659999" + suffix.substring(0, 4))
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

            deptA = departmentRepository.save(Department.builder()
                    .center(center)
                    .nameAr("قسم أ").nameEn("Dept A " + suffix)
                    .displayOrder(0).isActive(true)
                    .categories(new ArrayList<>())
                    .build());

            deptB = departmentRepository.save(Department.builder()
                    .center(center)
                    .nameAr("قسم ب").nameEn("Dept B " + suffix)
                    .displayOrder(1).isActive(true)
                    .categories(new ArrayList<>())
                    .build());
        });
    }

    @AfterEach
    void tearDown() {
        // Best-effort cleanup; pessimistic-locking integration tests have no transactional rollback.
        transactionTemplate.executeWithoutResult(status -> {
            // Departments/members/bookings cascade via FK ON DELETE in spec; explicit cleanup not strictly required.
            // Leaving residual test rows is acceptable in dev DB per the existing test pattern.
        });
    }

    /** SC-SC-2: 20 concurrent claims, exactly one wins. */
    @Test
    void twentyConcurrentClaims_exactlyOneSucceeds_othersGetAlreadyClaimed() throws Exception {
        int n = 20;

        // Setup: create the booking and 20 technicians, all in deptA, in one committed transaction.
        Long bookingId = transactionTemplate.execute(status -> {
            Booking booking = bookingRepository.save(Booking.builder()
                    .bookingNumber("SC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(10, 0))
                    .bookingStatus(BookingStatus.CONFIRMED)
                    .department(deptA)
                    .build());
            return booking.getId();
        });

        List<User> techs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int idx = i;
            User t = transactionTemplate.execute(status -> createTechnicianInDept(deptA, "ccA" + idx));
            techs.add(t);
        }

        // Concurrent claim
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger alreadyClaimed = new AtomicInteger();
        AtomicInteger otherFailures = new AtomicInteger();
        List<Throwable> unexpected = java.util.Collections.synchronizedList(new ArrayList<>());

        for (User t : techs) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    try {
                        bookingService.claim(bookingId, t);
                        successes.incrementAndGet();
                    } catch (ClaimException ce) {
                        if (ce.getCode() == ClaimErrorCode.BOOKING_ALREADY_CLAIMED) {
                            alreadyClaimed.incrementAndGet();
                        } else {
                            otherFailures.incrementAndGet();
                            unexpected.add(ce);
                        }
                    } catch (Throwable e) {
                        otherFailures.incrementAndGet();
                        unexpected.add(e);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        boolean finished = done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).as("all 20 claim attempts finished within 60s").isTrue();
        assertThat(unexpected).as("no unexpected errors").isEmpty();
        assertThat(successes.get()).as("exactly 1 claim succeeds").isEqualTo(1);
        assertThat(alreadyClaimed.get()).as("19 claims fail with BOOKING_ALREADY_CLAIMED").isEqualTo(n - 1);
        assertThat(otherFailures.get()).as("no other failure modes").isZero();

        // Booking must end up assigned to exactly one membership; one audit row must exist.
        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(finalBooking.getAssignedMembership())
                .as("booking has a single assigned technician")
                .isNotNull();
        assertThat(auditRepository.findByBookingId(bookingId))
                .as("exactly one audit row")
                .hasSize(1);
    }

    /** FR-SC-010 + scenario 8.3: cross-department claim is rejected with WRONG_DEPARTMENT. */
    @Test
    void crossDepartmentClaim_returnsWrongDepartment() {
        // Tech belongs to deptA. Booking belongs to deptB.
        User techInA = transactionTemplate.execute(status -> createTechnicianInDept(deptA, "xdA"));

        Long bookingId = transactionTemplate.execute(status -> {
            Booking booking = bookingRepository.save(Booking.builder()
                    .bookingNumber("SC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(11, 0))
                    .bookingStatus(BookingStatus.CONFIRMED)
                    .department(deptB)
                    .build());
            return booking.getId();
        });

        assertThatThrownBy(() -> bookingService.claim(bookingId, techInA))
                .isInstanceOfSatisfying(ClaimException.class,
                        ce -> assertThat(ce.getCode()).isEqualTo(ClaimErrorCode.WRONG_DEPARTMENT));

        // Booking must remain unassigned.
        Booking after = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(after.getAssignedMembership()).isNull();
    }

    /** Precondition 3: a CANCELLED booking is no longer claimable. */
    @Test
    void claimingCancelledBooking_returnsNotClaimable() {
        User tech = transactionTemplate.execute(status -> createTechnicianInDept(deptA, "cxA"));

        Long bookingId = transactionTemplate.execute(status -> {
            Booking booking = bookingRepository.save(Booking.builder()
                    .bookingNumber("SC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(12, 0))
                    .bookingStatus(BookingStatus.CANCELLED)
                    .department(deptA)
                    .build());
            return booking.getId();
        });

        assertThatThrownBy(() -> bookingService.claim(bookingId, tech))
                .isInstanceOfSatisfying(ClaimException.class,
                        ce -> assertThat(ce.getCode()).isEqualTo(ClaimErrorCode.BOOKING_NOT_CLAIMABLE));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User createTechnicianInDept(Department dept, String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Role staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STAFF").build()));

        User tech = userRepository.save(User.builder()
                .firstname("Tech-" + tag).lastname("Test")
                .email("sc-tech-" + tag + "-" + suffix + "@test.local")
                .password(passwordEncoder.encode("Test@1234"))
                .enabled(true).accountLocked(false)
                .userType(UserType.STAFF)
                .roles(List.of(staffRole))
                .build());

        CenterMembership m = CenterMembership.builder()
                .user(tech).center(center)
                .role(CenterRole.TECHNICIAN).status(MembershipStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .departments(new ArrayList<>(List.of(dept)))
                .build();
        membershipRepository.save(m);

        return tech;
    }
}
