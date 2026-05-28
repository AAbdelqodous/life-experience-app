package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingWorkProgressService {

    private final BookingWorkProgressRepository bookingWorkProgressRepository;
    private final BookingRepository bookingRepository;
    private final CenterSecurityService centerSecurity;

    public List<BookingWorkProgressResponse> getProgressForCustomer(Long bookingId, User customer) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to access this booking");
        }

        return bookingWorkProgressRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    public List<BookingWorkProgressResponse> getProgressForOwner(Long bookingId, User caller) {
        Booking booking = getBookingForCenterMember(bookingId, caller);
        return bookingWorkProgressRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId()).stream()
                .map(this::toOwnerResponse)
                .toList();
    }

    /**
     * Update the booking's current work stage and record a timeline entry capturing the transition.
     * <p>Enforces {@link WorkStage#canTransitionTo}: any non-listed transition is a 400. This is the
     * server-side enforcement promised by spec 009 FR-002 — the client's UI restriction is a UX
     * safeguard, not the source of truth.
     */
    @Transactional
    public void updateWorkStage(Long bookingId, User caller, UpdateWorkStageRequest request) {
        Booking booking = getBookingForWriter(bookingId, caller, CenterPermission.UPDATE_WORK_STAGE);

        WorkStage current = booking.getWorkStage();
        WorkStage target = request.getStage();

        // If a booking has no prior stage, the only valid first stage is RECEIVED.
        if (current == null) {
            if (target != WorkStage.RECEIVED) {
                throw new IllegalArgumentException(
                        "First stage must be RECEIVED; attempted: " + target);
            }
        } else if (!current.canTransitionTo(target)) {
            throw new IllegalArgumentException(
                    "Invalid stage transition: " + current + " → " + target
                            + ". Allowed next stages: " + current.getNextStageNames());
        }

        booking.setWorkStage(target);
        // JPA dirty-checking flushes booking.workStage at transaction commit.

        // Record a timeline entry so the Progress tab reflects the transition (spec FR-005).
        bookingWorkProgressRepository.save(BookingWorkProgress.builder()
                .booking(booking)
                .stage(target)
                .notes(request.getNotes())
                .notesAr(request.getNotesAr())
                .internalNotes(request.getInternalNotes())
                .estimatedMinutesRemaining(request.getEstimatedMinutesRemaining())
                .createdByName(displayName(caller))
                .build());

        log.info("Stage transition {} → {} for booking {} by user {}",
                current, target, bookingId, caller.getId());
    }

    @Transactional
    public BookingWorkProgressResponse createWorkProgress(Long bookingId, User caller,
                                                          CreateWorkProgressRequest request) {
        Booking booking = getBookingForWriter(bookingId, caller, CenterPermission.UPDATE_WORK_STAGE);

        BookingWorkProgress progress = BookingWorkProgress.builder()
                .booking(booking)
                .stage(booking.getWorkStage() != null ? booking.getWorkStage() : WorkStage.RECEIVED)
                .notes(request.getNotes())
                .notesAr(request.getNotesAr())
                .internalNotes(request.getInternalNotes())
                .estimatedMinutesRemaining(request.getEstimatedMinutesRemaining())
                .createdByName(displayName(caller))
                .build();

        BookingWorkProgress saved = bookingWorkProgressRepository.save(progress);
        log.info("Created progress entry id={} for booking {}", saved.getId(), bookingId);
        return toOwnerResponse(saved);
    }

    /**
     * Resolves the booking and verifies the caller can read it (any active member of the
     * booking's center). Used by GET endpoints.
     */
    private Booking getBookingForCenterMember(Long bookingId, User caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
        centerSecurity.requireActiveMembership(booking.getCenter().getId(), caller);
        return booking;
    }

    /**
     * Resolves the booking and verifies the caller may WRITE work-progress on it:
     * <ul>
     *   <li>they have an active membership at the booking's center,</li>
     *   <li>they hold the required permission (e.g. UPDATE_WORK_STAGE),</li>
     *   <li>if they are a TECHNICIAN, the booking is assigned to their membership.</li>
     * </ul>
     */
    private Booking getBookingForWriter(Long bookingId, User caller, CenterPermission permission) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        CenterMembership membership = centerSecurity.requirePermission(
                booking.getCenter().getId(), caller, permission);

        if (membership.getRole() == CenterRole.TECHNICIAN) {
            CenterMembership assigned = booking.getAssignedMembership();
            if (assigned == null || !assigned.getId().equals(membership.getId())) {
                throw new AccessDeniedException(
                        "Technicians may only update work progress on bookings assigned to them");
            }
        }
        return booking;
    }

    private String displayName(User u) {
        return (u.getFirstname() == null ? "" : u.getFirstname())
                + (u.getLastname() == null ? "" : " " + u.getLastname());
    }

    // Customer view: hides internalNotes
    private BookingWorkProgressResponse toCustomerResponse(BookingWorkProgress progress) {
        return BookingWorkProgressResponse.builder()
                .id(progress.getId())
                .bookingId(progress.getBooking().getId())
                .stage(progress.getStage())
                .notes(progress.getNotes())
                .notesAr(progress.getNotesAr())
                .photoUrl(progress.getPhotoUrl())
                .videoUrl(progress.getVideoUrl())
                .estimatedMinutesRemaining(progress.getEstimatedMinutesRemaining())
                .createdAt(progress.getCreatedAt())
                .createdByName(progress.getCreatedByName())
                .build();
    }

    private BookingWorkProgressResponse toOwnerResponse(BookingWorkProgress progress) {
        return BookingWorkProgressResponse.builder()
                .id(progress.getId())
                .bookingId(progress.getBooking().getId())
                .stage(progress.getStage())
                .notes(progress.getNotes())
                .notesAr(progress.getNotesAr())
                .internalNotes(progress.getInternalNotes())
                .photoUrl(progress.getPhotoUrl())
                .videoUrl(progress.getVideoUrl())
                .estimatedMinutesRemaining(progress.getEstimatedMinutesRemaining())
                .createdAt(progress.getCreatedAt())
                .createdByName(progress.getCreatedByName())
                .build();
    }
}
