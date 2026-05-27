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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingMediaService {

    /** Spec 009 edge case: photo > 10 MB rejected. Kept as bytes for clarity. */
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final BookingMediaRepository bookingMediaRepository;
    private final BookingRepository bookingRepository;
    private final CenterSecurityService centerSecurity;
    private final com.maintainance.service_center.config.FileStorageService fileStorageService;

    public List<BookingMediaResponse> getCustomerVisibleMedia(Long bookingId, User customer) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to access this booking");
        }

        return bookingMediaRepository.findByBookingIdAndIsVisibleToCustomerTrue(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookingMediaResponse> getAllMediaForOwner(Long bookingId, User caller) {
        Booking booking = getBookingForCenterMember(bookingId, caller);
        return bookingMediaRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingMediaResponse createMedia(Long bookingId, User caller, MultipartFile file,
            MediaCategory category, String caption, String captionAr, Boolean isVisibleToCustomer) {
        validateFile(file);
        Booking booking = getBookingForWriter(bookingId, caller, CenterPermission.UPLOAD_PROGRESS_MEDIA);

        String fileUrl = fileStorageService.storeFile(file);
        MediaType mediaType = determineMediaType(file.getContentType());

        BookingMedia media = BookingMedia.builder()
                .booking(booking)
                .mediaType(mediaType)
                .category(category)
                .url(fileUrl)
                .caption(caption)
                .captionAr(captionAr)
                .isVisibleToCustomer(isVisibleToCustomer != null ? isVisibleToCustomer : true)
                .uploadedBy(caller)
                .build();

        BookingMedia saved = bookingMediaRepository.save(media);
        log.info("Created media id={} for booking {} by user {}", saved.getId(), bookingId, caller.getId());
        return toResponse(saved);
    }

    @Transactional
    public BookingMediaResponse createCustomerMedia(Long bookingId, User customer, MultipartFile file,
            String caption, String captionAr) {
        validateFile(file);
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

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Photo is too large. Maximum size is 10 MB per photo.");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new IllegalArgumentException(
                    "Unsupported media type: " + contentType);
        }
    }

    private Booking getBookingForCenterMember(Long bookingId, User caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
        centerSecurity.requireActiveMembership(booking.getCenter().getId(), caller);
        return booking;
    }

    private Booking getBookingForWriter(Long bookingId, User caller, CenterPermission permission) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
        CenterMembership membership = centerSecurity.requirePermission(
                booking.getCenter().getId(), caller, permission);
        if (membership.getRole() == CenterRole.TECHNICIAN) {
            CenterMembership assigned = booking.getAssignedMembership();
            if (assigned == null || !assigned.getId().equals(membership.getId())) {
                throw new AccessDeniedException(
                        "Technicians may only upload media for bookings assigned to them");
            }
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
