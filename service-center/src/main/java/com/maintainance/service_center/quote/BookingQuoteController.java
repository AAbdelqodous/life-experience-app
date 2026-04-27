package com.maintainance.service_center.quote;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("bookings/{bookingId}/quotes")
@RequiredArgsConstructor
public class BookingQuoteController {

    private final BookingQuoteService quoteService;

    @GetMapping
    public ResponseEntity<List<BookingQuoteResponse>> getBookingQuotes(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(quoteService.getBookingQuotes(owner, bookingId));
    }

    @PostMapping
    public ResponseEntity<BookingQuoteResponse> createQuote(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateQuoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.createQuote(owner, bookingId, request));
    }

    @PostMapping("/{quoteId}/send")
    public ResponseEntity<BookingQuoteResponse> sendQuote(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId,
            @PathVariable Long quoteId) {
        return ResponseEntity.ok(quoteService.sendQuote(owner, bookingId, quoteId));
    }
}
