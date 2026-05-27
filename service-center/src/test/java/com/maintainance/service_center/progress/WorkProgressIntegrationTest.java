package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec 009 work-stage + work-progress backend behavior.
 * <p>Exercises the state machine enforcement (FR-002) and the side effect that every stage
 * transition also writes a {@link BookingWorkProgress} timeline entry (FR-005 / DoD step 3).
 */
@SpringBootTest
@ActiveProfiles("dev")
class WorkProgressIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingWorkProgressRepository progressRepository;
    @Autowired BookingWorkProgressService progressService;
    @Autowired TransactionTemplate transactionTemplate;

    private User owner;
    private User customer;
    private User stranger;
    private MaintenanceCenter center;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));

            owner = userRepository.save(User.builder()
                    .firstname("Wp").lastname("Owner")
                    .email("wp-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER)
                    .roles(List.of(ownerRole))
                    .build());

            customer = userRepository.save(User.builder()
                    .firstname("Wp").lastname("Customer")
                    .email("wp-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER)
                    .build());

            stranger = userRepository.save(User.builder()
                    .firstname("Wp").lastname("Stranger")
                    .email("wp-strg-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER)
                    .roles(List.of(ownerRole))
                    .build());

            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("ورشة " + suffix).nameEn("WP Center " + suffix)
                    .email("wp-c-" + suffix + "@test.local")
                    .phone("+96588888" + suffix.substring(0, 3))
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
        });
    }

    @Test
    void firstTransitionFromNull_mustBeReceived() {
        Long bookingId = createBooking(null);

        UpdateWorkStageRequest skipsReceived = new UpdateWorkStageRequest(
                WorkStage.DIAGNOSING, "trying to skip", null, null, null);
        assertThatThrownBy(() -> progressService.updateWorkStage(bookingId, owner, skipsReceived))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RECEIVED");

        UpdateWorkStageRequest received = new UpdateWorkStageRequest(
                WorkStage.RECEIVED, "car arrived", null, null, null);
        progressService.updateWorkStage(bookingId, owner, received);

        Booking after = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(after.getWorkStage()).isEqualTo(WorkStage.RECEIVED);
        assertThat(progressRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).hasSize(1);
    }

    @Test
    void invalidTransitionRejected_validTransitionWritesTimelineEntry() {
        // RECEIVED -> WORK_IN_PROGRESS is not allowed; only DIAGNOSING is.
        Long bookingId = createBooking(WorkStage.RECEIVED);

        UpdateWorkStageRequest invalid = new UpdateWorkStageRequest(
                WorkStage.WORK_IN_PROGRESS, "skip diagnose", null, null, null);
        assertThatThrownBy(() -> progressService.updateWorkStage(bookingId, owner, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RECEIVED")
                .hasMessageContaining("WORK_IN_PROGRESS");

        UpdateWorkStageRequest valid = new UpdateWorkStageRequest(
                WorkStage.DIAGNOSING, "starting diagnostic", "جاري الفحص", "ECU fault codes", 60);
        progressService.updateWorkStage(bookingId, owner, valid);

        Booking after = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(after.getWorkStage()).isEqualTo(WorkStage.DIAGNOSING);

        List<BookingWorkProgress> timeline =
                progressRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).getStage()).isEqualTo(WorkStage.DIAGNOSING);
        assertThat(timeline.get(0).getNotes()).isEqualTo("starting diagnostic");
        assertThat(timeline.get(0).getInternalNotes()).isEqualTo("ECU fault codes");
        assertThat(timeline.get(0).getEstimatedMinutesRemaining()).isEqualTo(60);
    }

    @Test
    void timelineEntriesOrderedOldestFirst() {
        Long bookingId = createBooking(WorkStage.RECEIVED);

        progressService.updateWorkStage(bookingId, owner,
                new UpdateWorkStageRequest(WorkStage.DIAGNOSING, "step 1", null, null, null));
        progressService.updateWorkStage(bookingId, owner,
                new UpdateWorkStageRequest(WorkStage.QUOTE_READY, "step 2", null, null, null));

        // Manual progress entry without stage transition
        progressService.createWorkProgress(bookingId, owner,
                CreateWorkProgressRequest.builder().notes("interim note").build());

        List<BookingWorkProgressResponse> entries =
                progressService.getProgressForOwner(bookingId, owner);
        assertThat(entries).hasSize(3);
        // Spec FR-004: oldest first
        assertThat(entries.get(0).getStage()).isEqualTo(WorkStage.DIAGNOSING);
        assertThat(entries.get(1).getStage()).isEqualTo(WorkStage.QUOTE_READY);
        assertThat(entries.get(2).getNotes()).isEqualTo("interim note");
    }

    @Test
    void nonCenterMember_cannotUpdateStage() {
        Long bookingId = createBooking(WorkStage.RECEIVED);

        UpdateWorkStageRequest req = new UpdateWorkStageRequest(
                WorkStage.DIAGNOSING, null, null, null, null);
        // The stranger has no membership at this center.
        assertThatThrownBy(() -> progressService.updateWorkStage(bookingId, stranger, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Long createBooking(WorkStage initialStage) {
        return transactionTemplate.execute(status -> {
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("WP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(10, 0))
                    .bookingStatus(BookingStatus.CONFIRMED)
                    .workStage(initialStage)
                    .build());
            return b.getId();
        });
    }
}
