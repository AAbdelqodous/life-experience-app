package com.maintainance.service_center.department;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Spec 020 — FR-D-013: backfill every existing center with a General department, then
 * route legacy memberships and non-terminal bookings into it. Idempotent: safe to re-run.
 * <p>Runs after AdminSeeder (which seeds roles + admin user) so centers can be resolved.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class DepartmentSeeder implements ApplicationRunner {

    private static final Set<BookingStatus> TERMINAL_STATUSES =
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);

    private final MaintenanceCenterRepository centerRepository;
    private final DepartmentService departmentService;
    private final CenterMembershipRepository membershipRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<MaintenanceCenter> centers = centerRepository.findAll();
        int seeded = 0, members = 0, bookings = 0;

        for (MaintenanceCenter center : centers) {
            Department general = departmentService.ensureGeneralDepartment(center);
            seeded++;

            // Assign all active TECHNICIAN memberships at the center to General if they
            // currently have no department assignments.
            List<CenterMembership> technicians = membershipRepository.findByCenterIdAndRoleAndStatus(
                    center.getId(), CenterRole.TECHNICIAN, MembershipStatus.ACTIVE);
            for (CenterMembership m : technicians) {
                if (m.getDepartments().isEmpty()) {
                    m.getDepartments().add(general);
                    membershipRepository.save(m);
                    members++;
                }
            }

            // Backfill any non-terminal bookings at this center that don't yet have a department.
            List<Booking> unrouted = bookingRepository.findUnroutedBookingsAtCenter(
                    center.getId(), TERMINAL_STATUSES);
            for (Booking b : unrouted) {
                b.setDepartment(general);
                bookings++;
            }
            if (!unrouted.isEmpty()) {
                bookingRepository.saveAll(unrouted);
            }
        }

        if (seeded + members + bookings > 0) {
            log.info("Department seeding complete: centers={} memberships_routed={} bookings_routed={}",
                    seeded, members, bookings);
        }
    }
}
