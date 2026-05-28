package com.maintainance.service_center.reroute;

import com.maintainance.service_center.booking.BookingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire shape returned by POST /bookings/{id}/reroute (spec 022 §Endpoint 3 Response).
 * The {@code updatedBooking} field carries the post-reroute snapshot so the frontend
 * doesn't need a separate re-fetch round trip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerouteResponse {
    private RerouteAuditResponse audit;
    private BookingResponse updatedBooking;
}
