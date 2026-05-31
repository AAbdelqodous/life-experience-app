package com.maintainance.service_center.inventory;

import com.maintainance.service_center.booking.BookingCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spec 025 (R5) — when a booking is cancelled, return any parts its quote consumed to stock. The
 * booking package publishes {@link BookingCancelledEvent}; this synchronous listener runs inside the
 * cancel transaction, so the compensating movements commit atomically. Net-based reversal, so it is a
 * no-op when no parts were consumed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationInventoryListener {

    private final InventoryService inventoryService;

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.debug("Booking {} cancelled → returning consumed parts to stock", event.bookingId());
        inventoryService.reverseConsumption(event.bookingId(), null);
    }
}
