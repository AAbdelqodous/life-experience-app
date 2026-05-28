package com.maintainance.service_center.reroute;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingResponse;
import com.maintainance.service_center.booking.BookingService;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.department.DepartmentRepository;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.notification.NotificationService;
import com.maintainance.service_center.quote.BookingQuoteService;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

/**
 * Spec 022: re-route a booking from one department to another within the same center.
 * <p>The 6 validations (caller permission, target dept validity, anti-diagnostic,
 * non-no-op, non-terminal status, concurrency) are evaluated under a pessimistic lock
 * to mirror the session-4 claim flow and prevent split-brain on simultaneous re-routes
 * by an assigned tech and a manager (research.md §Decision 4).
 * <p>The customer notification is enqueued AFTER COMMIT via
 * {@link TransactionSynchronizationManager} so a rolled-back re-route does not leave a
 * phantom "your booking moved" notification (research.md §Decision 5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RerouteService {

    private static final Set<BookingStatus> TERMINAL_STATUSES =
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);

    private final BookingRepository bookingRepository;
    private final DepartmentRepository departmentRepository;
    private final CenterMembershipRepository membershipRepository;
    private final RerouteAuditRepository rerouteAuditRepository;
    private final BookingService bookingService;
    private final BookingQuoteService quoteService;
    private final NotificationService notificationService;

    /**
     * Re-route a booking to a different active, non-diagnostic department at the same center.
     * <p>Returns the audit row + the post-reroute booking snapshot. Throws
     * {@link RerouteException} carrying the appropriate {@link BusinessErrorCodes} on
     * any validation failure (FR-DR-016 through FR-DR-019).
     */
    @Transactional
    public RerouteResponse reroute(Long bookingId, RerouteRequest request, User caller) {
        // Validate enum binding — Jackson will already reject unknown values; this catches
        // any explicit null that slipped past @NotNull (defensive).
        if (request.getReason() == null) {
            throw new RerouteException(BusinessErrorCodes.INVALID_REROUTE_REASON);
        }
        if (request.getNote() != null && request.getNote().length() > 500) {
            throw new RerouteException(BusinessErrorCodes.NOTE_TOO_LONG);
        }

        // Lock the booking row up front so concurrent reroutes serialise (research §Decision 4).
        // Reuses the same lock pattern as spec 021 claim.
        Booking booking = bookingRepository.findWithLockById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        Long centerId = booking.getCenter().getId();
        Department currentDept = booking.getDepartment();

        // Permission check (FR-DR-016): assigned tech OR caller has REROUTE_BOOKING_ANY at this center.
        CenterMembership callerMembership = resolveCallerMembership(centerId, caller);
        boolean isManagerWithAny = callerMembership != null
                && callerMembership.getRole().hasPermission(CenterPermission.REROUTE_BOOKING_ANY);
        boolean isAssignedTech = callerMembership != null
                && booking.getAssignedMembership() != null
                && callerMembership.getId().equals(booking.getAssignedMembership().getId())
                && callerMembership.getRole().hasPermission(CenterPermission.REROUTE_BOOKING_ASSIGNED);
        if (!isManagerWithAny && !isAssignedTech) {
            throw new RerouteException(BusinessErrorCodes.FORBIDDEN_REROUTE);
        }

        // Booking status guard (FR-DR-019).
        if (TERMINAL_STATUSES.contains(booking.getBookingStatus())) {
            throw new RerouteException(BusinessErrorCodes.INVALID_BOOKING_STATUS_FOR_REROUTE);
        }

        // Target dept must exist at the booking's center and be active.
        Department targetDept = departmentRepository.findByIdAndCenterId(
                        request.getTargetDepartmentId(), centerId)
                .orElseThrow(() -> new EntityNotFoundException("Target department not found"));
        if (!Boolean.TRUE.equals(targetDept.getIsActive())) {
            throw new EntityNotFoundException("Target department is not active");
        }

        // No-op (FR-DR-018): target equals current.
        if (currentDept != null && currentDept.getId().equals(targetDept.getId())) {
            throw new RerouteException(BusinessErrorCodes.NO_OP_REROUTE);
        }

        // Anti-diagnostic (FR-DR-017): reroute INTO diagnostic is forbidden.
        if (Boolean.TRUE.equals(targetDept.getIsDiagnostic())) {
            throw new RerouteException(BusinessErrorCodes.CANNOT_REROUTE_INTO_DIAGNOSTIC);
        }

        // First reroute on a booking originating in diagnostic → "initial classification" flag.
        boolean isInitialDiagnosticClassification =
                currentDept != null && Boolean.TRUE.equals(currentDept.getIsDiagnostic())
                        && rerouteAuditRepository.countByBookingId(bookingId) == 0;

        CenterMembership previouslyAssigned = booking.getAssignedMembership();

        // Persist audit row first — append-only, immutable (FR-DR-023, FR-DR-024).
        RerouteAudit audit = rerouteAuditRepository.save(RerouteAudit.builder()
                .booking(booking)
                .fromDepartment(currentDept)
                .toDepartment(targetDept)
                .fromMembership(previouslyAssigned)
                .triggeredBy(caller)
                .reason(request.getReason())
                .note(request.getNote())
                .isInitialDiagnosticClassification(isInitialDiagnosticClassification)
                .build());

        // Apply the re-route: change dept and unassign — booking returns to the target dept's
        // self-claim queue (FR-DR-020). Note: do NOT touch passedThroughDiagnostic — preserved.
        booking.setDepartment(targetDept);
        booking.setAssignedMembership(null);

        // Spec 022 FR-DR-021 — mark any in-force quote (SENT/APPROVED) as REVISED so the
        // customer is asked to re-approve. Implemented in spec 009's quote service.
        boolean hadActiveQuote = quoteService.markRevisedByBookingId(bookingId);

        // After-commit hook: deliver the customer notification only if the transaction
        // succeeds (research §Decision 5). Capture the values we need now — `booking` and
        // any other lazy entities are not safe to touch outside the transaction.
        User customer = booking.getCustomer();
        Long capturedBookingId = booking.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationService.notifyBookingRerouted(customer, capturedBookingId, hadActiveQuote);
                    } catch (Exception e) {
                        log.warn("Failed to enqueue BOOKING_REROUTED notification for booking {}: {}",
                                capturedBookingId, e.getMessage());
                    }
                }
            });
        }

        log.info("Re-routed booking {} from dept {} to dept {} reason={} by user {} (initialClassification={})",
                bookingId,
                currentDept != null ? currentDept.getId() : null,
                targetDept.getId(),
                request.getReason(),
                caller.getId(),
                isInitialDiagnosticClassification);

        return RerouteResponse.builder()
                .audit(RerouteAuditResponse.from(audit))
                .updatedBooking(bookingService.toResponse(booking))
                .build();
    }

    /**
     * GET /bookings/{id}/reroute-history — list every re-route on a booking chronologically.
     * Caller must have any active membership at the booking's center (read-side access);
     * a stricter permission gate could be added later if customer trust requires it.
     * <p>Read-only @Transactional keeps the Hibernate session open while we materialize
     * the lazy {@code from/toDepartment} proxies into {@link RerouteAuditResponse}.
     */
    @Transactional(readOnly = true)
    public java.util.List<RerouteAuditResponse> getHistory(Long bookingId, User caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        CenterMembership membership = resolveCallerMembership(booking.getCenter().getId(), caller);
        if (membership == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this booking's center");
        }
        // Technicians without REROUTE_BOOKING_ANY may read only their own bookings'
        // history. Managers (OWNER/BRANCH_MANAGER) carry REROUTE_BOOKING_ANY and pass.
        if (membership.getRole() == CenterRole.TECHNICIAN
                && !membership.getRole().hasPermission(CenterPermission.REROUTE_BOOKING_ANY)) {
            CenterMembership assigned = booking.getAssignedMembership();
            boolean wasAssigned = assigned != null && assigned.getId().equals(membership.getId());
            boolean historicallyAssigned = rerouteAuditRepository
                    .findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                    .anyMatch(a -> a.getFromMembership() != null
                            && a.getFromMembership().getId().equals(membership.getId()));
            if (!wasAssigned && !historicallyAssigned) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Technicians can only view re-route history for bookings they are or were assigned to");
            }
        }
        return rerouteAuditRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(RerouteAuditResponse::from)
                .toList();
    }

    private CenterMembership resolveCallerMembership(Long centerId, User caller) {
        return membershipRepository.findByCenterIdAndUserIdAndStatus(
                centerId, caller.getId(), MembershipStatus.ACTIVE).orElse(null);
    }
}
