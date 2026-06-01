package com.maintainance.service_center.quoterequest;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.chat.ChatService;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Spec 009/024 — locks the core marketplace loop logic without a database
 * (Mockito; runs anywhere). End-to-end DB behavior is covered by
 * {@code QuoteRequestIntegrationTest} (@SpringBootTest, dev profile + Postgres).
 */
@ExtendWith(MockitoExtension.class)
class QuoteRequestServiceTest {

    @Mock QuoteRequestRepository requestRepository;
    @Mock QuoteResponseRepository responseRepository;
    @Mock ServiceCategoryRepository categoryRepository;
    @Mock MaintenanceCenterRepository centerRepository;
    @Mock CenterMembershipRepository membershipRepository;
    @Mock CenterSecurityService centerSecurity;
    @Mock BookingRepository bookingRepository;
    @Mock ChatService chatService;
    @Mock com.maintainance.service_center.notification.NotificationService notificationService;

    @InjectMocks QuoteRequestService service;

    private User customer(int id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private ServiceCategory category(long id) {
        ServiceCategory c = new ServiceCategory();
        c.setId(id);
        c.setNameEn("AC");
        c.setNameAr("تكييف");
        return c;
    }

    private MaintenanceCenter center(long id) {
        MaintenanceCenter c = new MaintenanceCenter();
        c.setId(id);
        c.setNameEn("Gulf Center");
        c.setNameAr("مركز الخليج");
        c.setAverageRating(new BigDecimal("4.6"));
        return c;
    }

    private QuoteRequest openRequest(long id, User cust, ServiceCategory cat) {
        QuoteRequest r = new QuoteRequest();
        r.setId(id);
        r.setCustomer(cust);
        r.setCategory(cat);
        r.setDescription("AC not cold");
        r.setStatus(QuoteRequestStatus.OPEN);
        r.setExpiresAt(java.time.LocalDateTime.now().plusHours(48));
        r.setReachCount(3);
        return r;
    }

    @Test
    void createRequest_broadcasts_setsOpenAndReachCount() {
        User cust = customer(1);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L)));
        when(centerRepository.findByCategoryId(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(1), 3));
        when(requestRepository.save(any(QuoteRequest.class))).thenAnswer(i -> {
            QuoteRequest r = i.getArgument(0);
            r.setId(900L);
            return r;
        });

        var req = new CreateQuoteRequestRequest(5L, null, "AC not cold", null, null, "Hawalli", null);
        QuoteRequestResponse res = service.createRequest(cust, req);

        assertThat(res.status()).isEqualTo(QuoteRequestStatus.OPEN);
        assertThat(res.reachCount()).isEqualTo(3);
        assertThat(res.responses()).isEmpty();
    }

    @Test
    void acceptQuote_createsBooking_marksChosenSelected_othersNotSelected() {
        User cust = customer(1);
        ServiceCategory cat = category(5L);
        QuoteRequest request = openRequest(900L, cust, cat);

        QuoteResponse chosen = new QuoteResponse();
        chosen.setId(5001L);
        chosen.setRequest(request);
        chosen.setCenter(center(10L));
        chosen.setStatus(QuoteResponseStatus.SUBMITTED);
        chosen.setPriceMin(new BigDecimal("15.000"));
        chosen.setPriceMax(new BigDecimal("25.000"));

        QuoteResponse other = new QuoteResponse();
        other.setId(5002L);
        other.setRequest(request);
        other.setCenter(center(22L));
        other.setStatus(QuoteResponseStatus.SUBMITTED);

        when(requestRepository.findById(900L)).thenReturn(Optional.of(request));
        when(responseRepository.findById(5001L)).thenReturn(Optional.of(chosen));
        when(responseRepository.findByRequestId(900L)).thenReturn(List.of(chosen, other));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(7000L);
            return b;
        });

        AcceptResultResponse res = service.acceptQuote(cust, 900L, 5001L);

        assertThat(res.acceptedBookingId()).isEqualTo(7000L);
        assertThat(res.state()).isEqualTo(QuoteRequestStatus.ACCEPTED);
        assertThat(chosen.getStatus()).isEqualTo(QuoteResponseStatus.SELECTED);
        assertThat(other.getStatus()).isEqualTo(QuoteResponseStatus.NOT_SELECTED);
        assertThat(request.getStatus()).isEqualTo(QuoteRequestStatus.ACCEPTED);
        assertThat(request.getAcceptedBookingId()).isEqualTo(7000L);
    }

    @Test
    void acceptQuote_onClosedRequest_conflicts() {
        User cust = customer(1);
        QuoteRequest request = openRequest(900L, cust, category(5L));
        request.setStatus(QuoteRequestStatus.ACCEPTED);
        when(requestRepository.findById(900L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.acceptQuote(cust, 900L, 5001L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no longer open");
    }

    @Test
    void submitQuote_firstTime_isSubmitted_andOnePerCenter() {
        User owner = customer(2);
        MaintenanceCenter center = center(10L);
        QuoteRequest request = openRequest(900L, customer(1), category(5L));

        when(centerRepository.findFirstByOwnerId(2)).thenReturn(Optional.of(center));
        when(requestRepository.findById(900L)).thenReturn(Optional.of(request));
        when(responseRepository.findByRequestIdAndCenterId(900L, 10L)).thenReturn(Optional.empty());
        when(responseRepository.save(any(QuoteResponse.class))).thenAnswer(i -> {
            QuoteResponse r = i.getArgument(0);
            r.setId(6001L);
            return r;
        });

        var dto = new SubmitQuoteRequestDto(new BigDecimal("15.000"), new BigDecimal("25.000"), 90, "Gas refill", null);
        CenterQuoteResponseDto res = service.submitQuote(owner, 900L, dto);

        assertThat(res.status()).isEqualTo(QuoteResponseStatus.SUBMITTED);
        assertThat(res.priceMin()).isEqualByComparingTo("15.000");
    }

    @Test
    void submitQuote_rangeInverted_badRequest() {
        User owner = customer(2);
        when(centerRepository.findFirstByOwnerId(2)).thenReturn(Optional.of(center(10L)));
        when(requestRepository.findById(900L)).thenReturn(Optional.of(openRequest(900L, customer(1), category(5L))));

        var dto = new SubmitQuoteRequestDto(new BigDecimal("25.000"), new BigDecimal("15.000"), null, null, null);
        assertThatThrownBy(() -> service.submitQuote(owner, 900L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("priceMax");
    }

    @Test
    void withdrawQuote_whenSelected_conflicts() {
        User owner = customer(2);
        MaintenanceCenter center = center(10L);
        QuoteResponse selected = new QuoteResponse();
        selected.setId(6001L);
        selected.setCenter(center);
        selected.setStatus(QuoteResponseStatus.SELECTED);

        when(centerRepository.findFirstByOwnerId(2)).thenReturn(Optional.of(center));
        when(responseRepository.findByRequestIdAndCenterId(900L, 10L)).thenReturn(Optional.of(selected));

        assertThatThrownBy(() -> service.withdrawQuote(owner, 900L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("selected");
    }
}
