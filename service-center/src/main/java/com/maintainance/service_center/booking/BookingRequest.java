package com.maintainance.service_center.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "Center ID is required")
    private Long centerId;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotNull(message = "Booking time is required")
    private LocalTime bookingTime;

    private LocalTime estimatedEndTime;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    private String serviceDescription;

    private String problemDescription;

    private List<String> requestedServices;

    private String deviceType;

    private String deviceModel;

    private String deviceYear;

    private String deviceSerial;

    private List<String> problemImageUrls;

    private Double estimatedCost;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    private String customerAlternativePhone;

    private String customerAddress;

    private String specialInstructions;

    private Boolean isUrgent;

    private Boolean pickupRequired;

    private String pickupAddress;

    private PaymentMethod paymentMethod;
}
