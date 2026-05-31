package com.maintainance.service_center.booking;

import com.maintainance.service_center.address.Address;
import com.maintainance.service_center.progress.WorkStage;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class BookingResponse {
    private Long id;
    private String bookingNumber;
    private Integer customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long centerId;
    private String centerName;
    private String centerPhone;
    private Address centerAddress;
    private Double centerLatitude;
    private Double centerLongitude;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private LocalTime estimatedEndTime;
    private BookingStatus bookingStatus;
    private ServiceType serviceType;
    private String serviceDescription;
    private String problemDescription;
    private List<String> requestedServices;
    private String deviceType;
    private String deviceModel;
    private String deviceYear;
    private String deviceSerial;
    private List<String> problemImageUrls;
    private BigDecimal estimatedCost;
    private BigDecimal finalCost;
    private String costNotes;
    /** Spec 023 — deposit required at booking creation (null/zero = none). */
    private BigDecimal depositAmount;
    // Spec 008 — fulfillment choice + the fee snapshotted at creation, address, window, and leg.
    private FulfillmentMode fulfillmentMode;
    private BigDecimal fulfillmentFee;
    private ServiceAddress serviceAddress;
    private PickupWindow pickupWindow;
    private String logisticsState;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private String completionNotes;
    private List<String> completionImageUrls;
    private LocalDateTime cancelledAt;
    private String cancelledReason;
    private CancelledBy cancelledBy;
    private String customerAddress;
    private String specialInstructions;
    private Boolean isUrgent;
    private Boolean pickupRequired;
    private String pickupAddress;
    private WorkStage currentWorkStage;
    private ServiceSummary serviceSummary;
    private CategorySummary categorySummary;
    private Long assignedMembershipId;
    private String assignedStaffName;
    private Long departmentId;
    private String departmentNameAr;
    private String departmentNameEn;
    // Spec 022 — diagnostic intake fields. False/null on pre-022 bookings.
    private Boolean passedThroughDiagnostic;
    private BigDecimal diagnosticFeeRateAtClaim;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class ServiceSummary {
        private Long id;
        private String code;
        private String nameAr;
        private String nameEn;
    }

    @Getter
    @Builder
    public static class CategorySummary {
        private Long id;
        private String code;
        private String nameAr;
        private String nameEn;
    }
}
