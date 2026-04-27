package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingWorkProgressService {

    private final BookingWorkProgressRepository bookingWorkProgressRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceCenterRepository centerRepository;

    public List<BookingWorkProgressResponse> getProgressForCustomer(Long bookingId, User customer) {
        // Fetch booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found for ID: {}", bookingId);
                    return new EntityNotFoundException("Booking not found with ID: " + bookingId);
                });

        // Verify booking belongs to the authenticated customer
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            log.warn("Access denied: User {} attempted to access booking {} progress belonging to user {}", 
                    customer.getId(), bookingId, booking.getCustomer().getId());
            throw new AccessDeniedException("You do not have permission to access this booking");
        }

        // Fetch progress entries ordered by createdAt ASC
        List<BookingWorkProgress> progressList = bookingWorkProgressRepository
                .findByBookingIdOrderByCreatedAtAsc(bookingId);

        log.info("Retrieved {} progress entries for booking {} by user {}", 
                progressList.size(), bookingId, customer.getId());

        // Convert to response DTOs (internalNotes is excluded from the response DTO)
        return progressList.stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    public List<BookingWorkProgressResponse> getProgressForOwner(Long bookingId, User owner) {
        // Verify booking belongs to the authenticated center owner
        Booking booking = getBookingForOwner(bookingId, owner);

        // Fetch progress entries ordered by createdAt ASC
        List<BookingWorkProgress> progressList = bookingWorkProgressRepository
                .findByBookingIdOrderByCreatedAtAsc(bookingId);

        log.info("Retrieved {} progress entries for booking {} by owner {}", 
                progressList.size(), bookingId, owner.getId());

        // Convert to response DTOs (includes all fields including internalNotes)
        return progressList.stream()
                .map(this::toOwnerResponse)
                .toList();
    }

    @Transactional
    public void updateWorkStage(Long bookingId, User owner, UpdateWorkStageRequest request) {
        Booking booking = getBookingForOwner(bookingId, owner);
        booking.setWorkStage(request.getStage());
        log.info("Updated work stage to {} for booking {} by owner {}", request.getStage(), bookingId, owner.getId());
    }

    @Transactional
    public BookingWorkProgressResponse createWorkProgress(Long bookingId, User owner,
            CreateWorkProgressRequest request, String fileUrl) {
        // Verify booking belongs to the authenticated center owner
        Booking booking = getBookingForOwner(bookingId, owner);

        // Create work progress
        BookingWorkProgress progress = BookingWorkProgress.builder()
                .booking(booking)
                .stage(booking.getWorkStage() != null ? booking.getWorkStage() : WorkStage.RECEIVED)
                .notes(request.getNotes())
                .notesAr(request.getNotesAr())
                .internalNotes(request.getInternalNotes())
                .photoUrl(fileUrl)
                .estimatedMinutesRemaining(request.getEstimatedMinutesRemaining())
                .createdByName(owner.getFirstname() + " " + owner.getLastname())
                .build();

        BookingWorkProgress saved = bookingWorkProgressRepository.save(progress);

        log.info("Created work progress for booking {} by owner {}", bookingId, owner.getId());

        return toOwnerResponse(saved);
    }

    private Booking getBookingForOwner(Long bookingId, User owner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        MaintenanceCenter center = centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Maintenance center not found for owner"));

        if (!booking.getCenter().getId().equals(center.getId())) {
            throw new AccessDeniedException("You do not have access to this booking");
        }

        return booking;
    }

    // internalNotes intentionally excluded from customer view
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
