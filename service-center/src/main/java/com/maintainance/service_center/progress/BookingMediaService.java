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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingMediaService {

    private final BookingMediaRepository bookingMediaRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final com.maintainance.service_center.config.FileStorageService fileStorageService;

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

    public List<BookingMediaResponse> getAllMediaForOwner(Long bookingId, User owner) {
        // Verify booking belongs to the authenticated center owner
        Booking booking = getBookingForOwner(bookingId, owner);

        // Fetch all media
        List<BookingMedia> mediaList = bookingMediaRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);

        log.info("Retrieved {} media items for booking {} by owner {}", 
                mediaList.size(), bookingId, owner.getId());

        // Convert to response DTOs
        return mediaList.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingMediaResponse createMedia(Long bookingId, User owner, MultipartFile file,
            MediaCategory category, String caption, String captionAr, Boolean isVisibleToCustomer) {
        // Verify booking belongs to the authenticated center owner
        Booking booking = getBookingForOwner(bookingId, owner);

        // Store the file using FileStorageService
        String fileUrl = fileStorageService.storeFile(file);

        // Determine media type based on content type
        MediaType mediaType = determineMediaType(file.getContentType());

        BookingMedia media = BookingMedia.builder()
                .booking(booking)
                .mediaType(mediaType)
                .category(category)
                .url(fileUrl)
                .caption(caption)
                .captionAr(captionAr)
                .isVisibleToCustomer(isVisibleToCustomer != null ? isVisibleToCustomer : true)
                .uploadedBy(owner)
                .build();

        BookingMedia saved = bookingMediaRepository.save(media);

        log.info("Created media for booking {} by owner {}", bookingId, owner.getId());

        return toResponse(saved);
    }

    @Transactional
    public BookingMediaResponse createCustomerMedia(Long bookingId, User customer, MultipartFile file,
            String caption, String captionAr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have access to this booking");
        }

        String fileUrl = fileStorageService.storeFile(file);

        BookingMedia media = BookingMedia.builder()
                .booking(booking)
                .mediaType(determineMediaType(file.getContentType()))
                .category(MediaCategory.ISSUE_FOUND)
                .url(fileUrl)
                .caption(caption)
                .captionAr(captionAr)
                .isVisibleToCustomer(true)
                .uploadedBy(customer)
                .build();

        BookingMedia saved = bookingMediaRepository.save(media);
        log.info("Customer {} uploaded problem photo for booking {}", customer.getId(), bookingId);
        return toResponse(saved);
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

    private MediaType determineMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.PHOTO;
    }

    private BookingMediaResponse toResponse(BookingMedia media) {
        return BookingMediaResponse.builder()
                .id(media.getId())
                .bookingId(media.getBooking().getId())
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
