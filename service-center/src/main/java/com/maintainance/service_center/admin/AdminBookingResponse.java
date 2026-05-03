package com.maintainance.service_center.admin;

import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.booking.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingResponse {
    private Long id;
    private String customerName;
    private String customerEmail;
    private String centerNameAr;
    private String centerNameEn;
    private ServiceType serviceType;
    private BookingStatus bookingStatus;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private LocalDateTime createdAt;
}
