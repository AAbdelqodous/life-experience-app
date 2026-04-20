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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
