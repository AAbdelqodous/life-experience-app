package com.maintainance.service_center.complaint;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final BookingRepository bookingRepository;

    public ComplaintStatsResponse getMyStats(User user) {
        Integer userId = user.getId();
        return ComplaintStatsResponse.builder()
                .total(complaintRepository.countByUserId(userId))
                .pending(complaintRepository.countByUserIdAndStatus(userId, ComplaintStatus.PENDING))
                .underReview(complaintRepository.countByUserIdAndStatus(userId, ComplaintStatus.UNDER_REVIEW))
                .inProgress(complaintRepository.countByUserIdAndStatus(userId, ComplaintStatus.IN_PROGRESS))
                .resolved(complaintRepository.countByUserIdAndStatus(userId, ComplaintStatus.RESOLVED))
                .closed(complaintRepository.countByUserIdAndStatus(userId, ComplaintStatus.CLOSED))
                .escalated(complaintRepository.countByUserIdAndStatus(userId, ComplaintStatus.ESCALATED))
                .build();
    }

    @Transactional
    public ComplaintResponse fileComplaint(ComplaintRequest request, User user) {
        log.info("Filing complaint for center {} by user {}", request.getCenterId(), user.getId());

        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found"));

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        }

        // Generate unique complaint number
        String complaintNumber = generateComplaintNumber();

        Complaint complaint = Complaint.builder()
                .complaintNumber(complaintNumber)
                .complainant(user)
                .center(center)
                .booking(booking)
                .type(request.getType())
                .status(ComplaintStatus.PENDING)
                .priority(ComplaintPriority.LOW)
                .subject(request.getSubject())
                .description(request.getDescription())
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        log.info("Complaint filed successfully with number {}", complaintNumber);

        return mapToResponse(savedComplaint);
    }

    public PageResponse<ComplaintResponse> getMyComplaints(User user, int page, int size, String sortBy, String sortOrder) {
        log.info("Fetching complaints for user {}, page {}, size {}, sort by {} {}", user.getId(), page, size, sortBy, sortOrder);

        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Complaint> complaints = complaintRepository.findByComplainantIdOrderByCreatedAtDesc(user.getId(), pageable);

        return PageResponse.of(complaints.map(this::mapToResponse));
    }

    public ComplaintResponse getComplaintById(Long id, User user) {
        log.info("Fetching complaint {} for user {}", id, user.getId());

        Complaint complaint = complaintRepository.findByIdAndComplainantId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found"));

        return mapToResponse(complaint);
    }

    private String generateComplaintNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CPL-" + timestamp + "-" + uuid;
    }

    private ComplaintResponse mapToResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .complaintNumber(complaint.getComplaintNumber())
                .type(complaint.getType())
                .subject(complaint.getSubject())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .resolution(complaint.getResolution())
                .centerId(complaint.getCenter().getId())
                .centerNameAr(complaint.getCenter().getNameAr())
                .centerNameEn(complaint.getCenter().getNameEn())
                .bookingId(complaint.getBooking() != null ? complaint.getBooking().getId() : null)
                .createdAt(complaint.getCreatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }
}
