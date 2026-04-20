package com.maintainance.service_center.booking;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MaintenanceCenterRepository centerRepository;

    public Page<BookingResponse> findByCustomer(User customer, Pageable pageable) {
        return bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(this::toResponse);
    }

    public Page<BookingResponse> findByCenter(Long centerId, User caller, Pageable pageable) {
        MaintenanceCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + centerId));
        if (center.getOwner() == null || !center.getOwner().getId().equals(caller.getId())) {
            throw new AccessDeniedException("You do not have permission to view bookings for this center");
        }
        return bookingRepository.findByCenter_IdOrderByCreatedAtDesc(centerId, pageable)
                .map(this::toResponse);
    }

    public BookingResponse findByBookingNumber(String bookingNumber) {
        return toResponse(getBookingByNumber(bookingNumber));
    }

    public BookingResponse findById(Long id, User caller) {
        Booking booking = getBooking(id);
        checkOwnershipOrAccess(booking, caller);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse create(BookingRequest request, User customer) {
        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + request.getCenterId()));

        if (!center.getIsActive()) {
            throw new IllegalArgumentException("Cannot create booking for inactive center");
        }

        String bookingNumber = generateBookingNumber();

        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .customer(customer)
                .center(center)
                .bookingDate(request.getBookingDate())
                .bookingTime(request.getBookingTime())
                .estimatedEndTime(request.getEstimatedEndTime())
                .bookingStatus(BookingStatus.PENDING)
                .serviceType(request.getServiceType())
                .serviceDescription(request.getServiceDescription())
                .problemDescription(request.getProblemDescription())
                .requestedServices(request.getRequestedServices() != null ? request.getRequestedServices() : List.of())
                .deviceType(request.getDeviceType())
                .deviceModel(request.getDeviceModel())
                .deviceYear(request.getDeviceYear())
                .deviceSerial(request.getDeviceSerial())
                .problemImageUrls(request.getProblemImageUrls() != null ? request.getProblemImageUrls() : List.of())
                .estimatedCost(request.getEstimatedCost())
                .customerPhone(request.getCustomerPhone())
                .customerAlternativePhone(request.getCustomerAlternativePhone())
                .customerAddress(request.getCustomerAddress())
                .specialInstructions(request.getSpecialInstructions())
                .isUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : false)
                .pickupRequired(request.getPickupRequired() != null ? request.getPickupRequired() : false)
                .pickupAddress(request.getPickupAddress())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        bookingRepository.save(booking);
        log.info("Created booking number={} for customer id={} at center id={}", 
                bookingNumber, customer.getId(), center.getId());
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse update(Long id, BookingRequest request, User caller) {
        Booking booking = getBooking(id);
        checkOwnershipOrAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Cannot update booking that is not pending");
        }

        if (request.getCenterId() != null && !booking.getCenter().getId().equals(request.getCenterId())) {
            MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                    .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + request.getCenterId()));
            if (!center.getIsActive()) {
                throw new IllegalArgumentException("Cannot update booking to inactive center");
            }
            booking.setCenter(center);
        }

        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setEstimatedEndTime(request.getEstimatedEndTime());
        booking.setServiceType(request.getServiceType());
        booking.setServiceDescription(request.getServiceDescription());
        booking.setProblemDescription(request.getProblemDescription());
        booking.setRequestedServices(request.getRequestedServices() != null ? request.getRequestedServices() : List.of());
        booking.setDeviceType(request.getDeviceType());
        booking.setDeviceModel(request.getDeviceModel());
        booking.setDeviceYear(request.getDeviceYear());
        booking.setDeviceSerial(request.getDeviceSerial());
        booking.setProblemImageUrls(request.getProblemImageUrls() != null ? request.getProblemImageUrls() : List.of());
        booking.setEstimatedCost(request.getEstimatedCost());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setCustomerAlternativePhone(request.getCustomerAlternativePhone());
        booking.setCustomerAddress(request.getCustomerAddress());
        booking.setSpecialInstructions(request.getSpecialInstructions());
        booking.setIsUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : booking.getIsUrgent());
        booking.setPickupRequired(request.getPickupRequired() != null ? request.getPickupRequired() : booking.getPickupRequired());
        booking.setPickupAddress(request.getPickupAddress());
        booking.setPaymentMethod(request.getPaymentMethod());

        bookingRepository.save(booking);
        log.info("Updated booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse confirm(Long id, User caller) {
        Booking booking = getBooking(id);
        checkCenterAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Can only confirm pending bookings");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.info("Confirmed booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse startService(Long id, User caller) {
        Booking booking = getBooking(id);
        checkCenterAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Can only start confirmed bookings");
        }

        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(booking);
        log.info("Started service for booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse complete(Long id, BookingCompletionRequest request, User caller) {
        Booking booking = getBooking(id);
        checkCenterAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Can only complete in-progress bookings");
        }

        PaymentStatus resolvedPaymentStatus = request.getPaymentStatus() != null
                ? request.getPaymentStatus()
                : PaymentStatus.PAID;

        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking.setCompletionNotes(request.getCompletionNotes());
        booking.setFinalCost(request.getFinalCost());
        booking.setCostNotes(request.getCostNotes());
        booking.setCompletionImageUrls(request.getCompletionImageUrls() != null ? request.getCompletionImageUrls() : List.of());
        booking.setPaymentStatus(resolvedPaymentStatus);
        if (resolvedPaymentStatus == PaymentStatus.PAID) {
            booking.setPaidAt(LocalDateTime.now());
        }

        bookingRepository.save(booking);
        log.info("Completed booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(Long id, BookingCancellationRequest request, User caller) {
        Booking booking = getBooking(id);
        checkOwnershipOrAccess(booking, caller);

        if (booking.getBookingStatus() == BookingStatus.COMPLETED || 
            booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel completed or cancelled booking");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledReason(request.getReason());
        booking.setCancelledBy(caller.getId().equals(booking.getCustomer().getId()) ? 
                CancelledBy.CUSTOMER : CancelledBy.CENTER);

        bookingRepository.save(booking);
        log.info("Cancelled booking id={} by {}", id, booking.getCancelledBy());
        return toResponse(booking);
    }

    public BookingStatsResponse getMyStats(User customer) {
        Integer customerId = customer.getId();
        return BookingStatsResponse.builder()
                .total(bookingRepository.countByCustomerId(customerId))
                .pending(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.PENDING))
                .confirmed(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.CONFIRMED))
                .inProgress(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.IN_PROGRESS))
                .completed(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.COMPLETED))
                .cancelled(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.CANCELLED))
                .noShow(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.NO_SHOW))
                .rescheduled(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.RESCHEDULED))
                .totalRevenue(nullSafe(bookingRepository.sumFinalCostByCustomerId(customerId)))
                .build();
    }

    public BookingStatsResponse getCenterStats(User caller) {
        if (caller == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        
        // Verify user is a center owner
        if (caller.getUserType() != UserType.CENTER_OWNER) {
            throw new AccessDeniedException("Only center owners can access center statistics");
        }
        
        Integer ownerId = caller.getId();
        return BookingStatsResponse.builder()
                .total(bookingRepository.countByOwnerId(ownerId))
                .pending(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.PENDING))
                .confirmed(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.CONFIRMED))
                .inProgress(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.IN_PROGRESS))
                .completed(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.COMPLETED))
                .cancelled(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.CANCELLED))
                .noShow(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.NO_SHOW))
                .rescheduled(bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.RESCHEDULED))
                .totalRevenue(nullSafe(bookingRepository.sumFinalCostByOwnerId(ownerId)))
                .build();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public List<BookingResponse> getMyBookings(User customer, BookingStatus status) {
        return bookingRepository.findByCustomerIdAndStatuses(customer.getId(), List.of(status))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + id));
    }

    private Booking getBookingByNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with number: " + bookingNumber));
    }

    private void checkOwnershipOrAccess(Booking booking, User caller) {
        boolean isCustomer = caller.getId().equals(booking.getCustomer().getId());
        boolean isCenterOwner = booking.getCenter().getOwner() != null &&
                caller.getId().equals(booking.getCenter().getOwner().getId());
        if (!isCustomer && !isCenterOwner) {
            throw new IllegalArgumentException("You do not have permission to access this booking");
        }
    }

    private void checkCenterAccess(Booking booking, User caller) {
        if (booking.getCenter().getOwner() == null ||
                !caller.getId().equals(booking.getCenter().getOwner().getId())) {
            throw new AccessDeniedException("You do not have permission to modify this booking");
        }
    }

    private String generateBookingNumber() {
        String number;
        do {
            number = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingNumber(number));
        return number;
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().fullName())
                .customerEmail(booking.getCustomer().getEmail())
                .customerPhone(booking.getCustomerPhone())
                .centerId(booking.getCenter().getId())
                .centerName(booking.getCenter().getNameEn())
                .centerPhone(booking.getCenter().getPhone())
                .centerAddress(booking.getCenter().getAddress())
                .centerLatitude(booking.getCenter().getLatitude())
                .centerLongitude(booking.getCenter().getLongitude())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .estimatedEndTime(booking.getEstimatedEndTime())
                .bookingStatus(booking.getBookingStatus())
                .serviceType(booking.getServiceType())
                .serviceDescription(booking.getServiceDescription())
                .problemDescription(booking.getProblemDescription())
                .requestedServices(booking.getRequestedServices())
                .deviceType(booking.getDeviceType())
                .deviceModel(booking.getDeviceModel())
                .deviceYear(booking.getDeviceYear())
                .deviceSerial(booking.getDeviceSerial())
                .problemImageUrls(booking.getProblemImageUrls())
                .estimatedCost(booking.getEstimatedCost())
                .finalCost(booking.getFinalCost())
                .costNotes(booking.getCostNotes())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .paidAt(booking.getPaidAt())
                .completedAt(booking.getCompletedAt())
                .completionNotes(booking.getCompletionNotes())
                .completionImageUrls(booking.getCompletionImageUrls())
                .cancelledAt(booking.getCancelledAt())
                .cancelledReason(booking.getCancelledReason())
                .cancelledBy(booking.getCancelledBy())
                .customerAddress(booking.getCustomerAddress())
                .specialInstructions(booking.getSpecialInstructions())
                .isUrgent(booking.getIsUrgent())
                .pickupRequired(booking.getPickupRequired())
                .pickupAddress(booking.getPickupAddress())
                .currentWorkStage(booking.getCurrentWorkStage())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
