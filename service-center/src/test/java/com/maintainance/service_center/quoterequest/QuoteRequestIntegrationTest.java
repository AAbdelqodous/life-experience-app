package com.maintainance.service_center.quoterequest;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 009/024 — end-to-end marketplace loop against the real DB (dev profile + Postgres):
 * customer broadcasts → center quotes → it appears in the center inbox → customer accepts →
 * a Booking is created (carrying originRequestId) and the chosen quote is SELECTED.
 * Requires a running Postgres (`docker-compose up`); not run in CI sandboxes without a DB.
 */
@SpringBootTest
@ActiveProfiles("dev")
class QuoteRequestIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ServiceCategoryRepository categoryRepository;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired QuoteRequestRepository requestRepository;
    @Autowired QuoteResponseRepository responseRepository;
    @Autowired QuoteRequestService service;
    @Autowired TransactionTemplate tx;

    private User owner;
    private User customer;
    private MaintenanceCenter center;
    private ServiceCategory category;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));

            owner = userRepository.save(User.builder()
                    .firstname("QR").lastname("Owner")
                    .email("qr-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER).roles(List.of(ownerRole))
                    .build());

            customer = userRepository.save(User.builder()
                    .firstname("QR").lastname("Customer")
                    .email("qr-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER)
                    .build());

            category = new ServiceCategory();
            category.setCode("QR_CAT_" + suffix);
            category.setNameAr("تكييف");
            category.setNameEn("AC");
            category.setIsActive(true);
            category = categoryRepository.save(category);

            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز " + suffix).nameEn("QR Center " + suffix)
                    .email("qr-c-" + suffix + "@test.local")
                    .phone("+96577666" + suffix.substring(0, 3))
                    .categories(new ArrayList<>(List.of(category)))
                    .workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>())
                    .imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>())
                    .owner(owner)
                    .isVerified(false).isActive(true).enabled(true)
                    .build());

            membershipRepository.save(CenterMembership.builder()
                    .user(owner).center(center)
                    .role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now()).build());
        });
    }

    @Test
    void fullLoop_broadcast_quote_inbox_accept_createsBooking() {
        // 1) Customer broadcasts a request in the center's category.
        var create = new CreateQuoteRequestRequest(
                category.getId(), null, "AC not cold", null, "Camry", "Hawalli", FulfillmentHint.PICKUP_DELIVERY);
        QuoteRequestResponse created = service.createRequest(customer, create);
        assertThat(created.status()).isEqualTo(QuoteRequestStatus.OPEN);
        assertThat(created.reachCount()).isGreaterThanOrEqualTo(1);
        Long requestId = created.id();

        // 2) Center (owner) submits a quote.
        var quote = new SubmitQuoteRequestDto(new BigDecimal("15.000"), new BigDecimal("25.000"), 90, "Gas refill", "Tomorrow AM");
        CenterQuoteResponseDto submitted = service.submitQuote(owner, requestId, quote);
        assertThat(submitted.status()).isEqualTo(QuoteResponseStatus.SUBMITTED);

        // 3) It appears in the center inbox, marked SUBMITTED.
        List<InboxItemResponse> inbox = service.getInbox(owner);
        assertThat(inbox).anyMatch(i -> i.requestId().equals(requestId)
                && "SUBMITTED".equals(i.myResponseStatus()));

        // 4) Customer sees the quote in the full view and accepts it.
        QuoteRequestResponse customerView = (QuoteRequestResponse) service.getForCaller(customer, requestId);
        assertThat(customerView.responses()).hasSize(1);
        Long quoteId = customerView.responses().get(0).id();

        AcceptResultResponse accept = service.acceptQuote(customer, requestId, quoteId);
        assertThat(accept.state()).isEqualTo(QuoteRequestStatus.ACCEPTED);
        assertThat(accept.acceptedBookingId()).isNotNull();

        // 5) A booking was created carrying originRequestId; the chosen quote is SELECTED.
        Booking booking = bookingRepository.findById(accept.acceptedBookingId()).orElseThrow();
        assertThat(booking.getOriginRequestId()).isEqualTo(requestId);
        assertThat(booking.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(responseRepository.findById(quoteId).orElseThrow().getStatus())
                .isEqualTo(QuoteResponseStatus.SELECTED);
    }

    @Test
    void centerView_isSealed_toOwnQuoteOnly() {
        var create = new CreateQuoteRequestRequest(category.getId(), null, "Noise", null, null, "Hawalli", null);
        Long requestId = service.createRequest(customer, create).id();
        service.submitQuote(owner, requestId, new SubmitQuoteRequestDto(
                new BigDecimal("10.000"), new BigDecimal("10.000"), 60, null, null));

        Object view = service.getForCaller(owner, requestId);
        assertThat(view).isInstanceOf(QuoteRequestDetailResponse.class);
        QuoteRequestDetailResponse detail = (QuoteRequestDetailResponse) view;
        assertThat(detail.myResponse()).isNotNull();
        assertThat(detail.myResponse().priceMin()).isEqualByComparingTo("10.000");
    }
}
