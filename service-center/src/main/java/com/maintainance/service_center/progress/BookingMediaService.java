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
public class BookingMediaService {

    private final BookingMediaRepository bookingMediaRepository;
    private final BookingRepository bookingRepository;

    /**
     * Get customer-visible media for a booking
     * 
     * @param bookingId the booking ID
     * @param customer the authenticated customer
     * @return list of booking media responses visible to the customer
     * @throws EntityNotFoundException if booking not found
     * @throws AccessDeniedException if booking does not belong to the customer
     */
    public List<BookingMediaResponse> getCustomerVisibleMedia(Long bookingId, User customer) {
        // Fetch booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found for ID: {}", bookingId);
                    return new EntityNotFoundException("Booking not found with ID: " + bookingId);
                });

        // Verify booking belongs to the authenticated customer
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            log.warn("Access denied: User {} attempted to access booking {} belonging to user {}", 
                    customer.getId(), bookingId, booking.getCustomer().getId());
            throw new AccessDeniedException("You do not have permission to access this booking");
        }

        // Fetch customer-visible media
        List<BookingMedia> mediaList = bookingMediaRepository.findByBookingIdAndIsVisibleToCustomerTrue(bookingId);

        log.info("Retrieved {} customer-visible media items for booking {} by user {}", 
                mediaList.size(), bookingId, customer.getId());

        // Convert to response DTOs
        return mediaList.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Map BookingMedia entity to BookingMediaResponse DTO
     */
    private BookingMediaResponse toResponse(BookingMedia media) {
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
