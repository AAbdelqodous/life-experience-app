package com.maintainance.service_center.progress;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
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

    /**
     * Get work progress for a booking (customer-facing)
     * 
     * @param bookingId the booking ID
     * @param customer the authenticated customer
     * @return list of booking work progress responses ordered by createdAt ASC
     * @throws EntityNotFoundException if booking not found
     * @throws AccessDeniedException if booking does not belong to the customer
     */
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
                .map(this::toResponse)
                .toList();
    }

    /**
     * Map BookingWorkProgress entity to BookingWorkProgressResponse DTO
     * Note: internalNotes is intentionally excluded from the response
     */
    private BookingWorkProgressResponse toResponse(BookingWorkProgress progress) {
        return BookingWorkProgressResponse.builder()
                .id(progress.getId())
                .stage(progress.getStage())
                .notes(progress.getNotes())
                .notesAr(progress.getNotesAr())
                .estimatedMinutesRemaining(progress.getEstimatedMinutesRemaining())
                .photos(progress.getMedia().stream()
                        .map(this::toMediaResponse)
                        .collect(Collectors.toList()))
                .createdAt(progress.getCreatedAt())
                .createdByName(progress.getCreatedByName())
                .build();
    }

    /**
     * Map BookingMedia entity to BookingMediaResponse DTO
     */
    private BookingMediaResponse toMediaResponse(BookingMedia media) {
        return BookingMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .category(media.getCategory())
                .url(media.getUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .caption(media.getCaption())
                .captionAr(media.getCaptionAr())
                .isVisibleToCustomer(media.getIsVisibleToCustomer())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
