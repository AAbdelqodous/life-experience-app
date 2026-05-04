package com.maintainance.service_center.admin;

import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.complaint.ComplaintRepository;
import com.maintainance.service_center.complaint.ComplaintStatus;
import com.maintainance.service_center.review.ReviewRepository;
import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ComplaintRepository complaintRepository;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getPlatformOverview() {
        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDateTime.of(now, LocalTime.MAX);

        log.info("Fetching platform analytics overview");

        // User statistics
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByUserType(UserType.CUSTOMER);
        long totalOwners = userRepository.countByUserType(UserType.OWNER);
        long newUsersThisMonth = userRepository.countByCreatedDateBetween(monthStart, monthEnd);

        // Center statistics
        long totalCenters = centerRepository.count();
        long approvedCenters = centerRepository.countByOwnerApprovalStatus(ApprovalStatus.APPROVED);
        long pendingCenters = centerRepository.countByOwnerApprovalStatus(ApprovalStatus.PENDING_APPROVAL);

        // Booking statistics
        long totalBookings = bookingRepository.count();
        long completedBookings = bookingRepository.countByBookingStatus(BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByBookingStatus(BookingStatus.CANCELLED);
        long bookingsThisMonth = bookingRepository.countByCreatedAtBetween(monthStart, monthEnd);

        // Review statistics
        long totalReviews = reviewRepository.count();
        Double averagePlatformRating = reviewRepository.calculatePlatformAverageRating();

        // Complaint statistics
        long totalComplaints = complaintRepository.count();
        List<ComplaintStatus> openStatuses = List.of(
            ComplaintStatus.PENDING,
            ComplaintStatus.UNDER_REVIEW,
            ComplaintStatus.IN_PROGRESS,
            ComplaintStatus.ESCALATED
        );
        long openComplaints = complaintRepository.countByStatusIn(openStatuses);

        return AdminAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalOwners(totalOwners)
                .newUsersThisMonth(newUsersThisMonth)
                .totalCenters(totalCenters)
                .approvedCenters(approvedCenters)
                .pendingCenters(pendingCenters)
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .bookingsThisMonth(bookingsThisMonth)
                .totalReviews(totalReviews)
                .averagePlatformRating(averagePlatformRating)
                .totalComplaints(totalComplaints)
                .openComplaints(openComplaints)
                .build();
    }
}
