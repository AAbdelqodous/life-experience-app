package com.maintainance.service_center.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.quote.BookingQuote;
import com.maintainance.service_center.quote.BookingQuoteRepository;
import com.maintainance.service_center.quote.QuoteLineItem;
import com.maintainance.service_center.quote.QuoteStatus;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.security.JwtService;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 007/023 — HTTP-level contract test: the real controllers + security (JwtFilter →
 * @AuthenticationPrincipal) + JSON serialization, exercised with a real JWT, asserting the exact
 * field names and status codes the customer/center frontends consume. Requires a running Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PaymentHttpContractTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingQuoteRepository quoteRepository;
    @Autowired TransactionTemplate tx;

    private String customerToken;
    private String ownerToken;
    private Long bookingId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
            User owner = userRepository.save(User.builder()
                    .firstname("Http").lastname("Owner").email("http-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.OWNER).roles(List.of(ownerRole)).build());
            User customer = userRepository.save(User.builder()
                    .firstname("Http").lastname("Cust").email("http-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER).roles(List.of(userRole)).build());
            MaintenanceCenter center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز " + suffix).nameEn("Http Center " + suffix)
                    .email("http-c-" + suffix + "@test.local").phone("+96550333" + suffix.substring(0, 3))
                    .categories(new ArrayList<>()).workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>()).imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>()).owner(owner)
                    .isVerified(false).isActive(true).enabled(true).build());
            membershipRepository.save(CenterMembership.builder()
                    .user(owner).center(center).role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now()).build());
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("HTTP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer).center(center)
                    .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                    .bookingStatus(BookingStatus.IN_PROGRESS).build());
            QuoteLineItem li = new QuoteLineItem();
            li.setDescription("AC repair");
            li.setDescriptionAr("إصلاح مكيف");
            li.setPartsCost(new BigDecimal("18.000"));
            li.setLaborCost(new BigDecimal("10.500"));
            BookingQuote q = new BookingQuote();
            q.setBooking(b);
            q.setLineItems(List.of(li));
            q.setSubtotal(new BigDecimal("28.500"));
            q.setTotalAmount(new BigDecimal("28.500"));
            q.setStatus(QuoteStatus.APPROVED);
            quoteRepository.save(q);

            bookingId = b.getId();
            customerToken = jwtService.generateToken(customer);
            ownerToken = jwtService.generateToken(owner);
        });
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void payFlow_overHttp_matchesFrontendContract() throws Exception {
        // 1) Invoice (customer) — exact keys the customer 007 slice reads.
        mockMvc.perform(get("/bookings/" + bookingId + "/invoice").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId", is(bookingId.intValue())))
                .andExpect(jsonPath("$.total", is(28.5)))
                .andExpect(jsonPath("$.currency", is("KWD")))
                .andExpect(jsonPath("$.paymentStatus", is("PENDING")))
                .andExpect(jsonPath("$.walletApplicable", is(true)))
                .andExpect(jsonPath("$.releaseEligible", is(false)))
                .andExpect(jsonPath("$.availableMethods", hasItem("KNET")))
                .andExpect(jsonPath("$.lines[0].labelEn", notNullValue()));

        // 2) Initiate (customer) → 201 with paymentId/status/checkoutUrl/returnUrlPrefix.
        String body = objectMapper.writeValueAsString(Map.of(
                "bookingId", bookingId, "method", "KNET", "useWalletBalance", false, "idempotencyKey", "http-" + bookingId));
        var initResult = mockMvc.perform(post("/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", greaterThan(0)))
                .andExpect(jsonPath("$.status", is("HELD")))
                .andExpect(jsonPath("$.checkoutUrl", notNullValue()))
                .andExpect(jsonPath("$.returnUrlPrefix", notNullValue()))
                .andReturn();
        JsonNode init = objectMapper.readTree(initResult.getResponse().getContentAsString());
        long paymentId = init.get("paymentId").asLong();

        // 3) Status poll (customer).
        mockMvc.perform(get("/payments/" + paymentId).header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is((int) paymentId)))
                .andExpect(jsonPath("$.status", is("HELD")))
                .andExpect(jsonPath("$.bookingId", is(bookingId.intValue())));

        // 4) Center completes the booking (owner) — the escrow release-trigger is folded into the
        //    booking-lifecycle completion (no separate /mark-complete endpoint). Completion alone
        //    must flip the held payment to release-eligible; step 5 proves it did.
        String completeBody = objectMapper.writeValueAsString(Map.of("finalCost", 28.500, "paymentStatus", "PENDING"));
        mockMvc.perform(post("/bookings/" + bookingId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(completeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus", is("COMPLETED")));

        // 5) Customer releases → RELEASED (only possible because completion made it release-eligible).
        mockMvc.perform(post("/bookings/" + bookingId + "/release").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("RELEASED")));

        // 6) Center earnings (owner) — net 27.075 now Available.
        mockMvc.perform(get("/centers/my/earnings").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(27.075)))
                .andExpect(jsonPath("$.held", is(0)))
                .andExpect(jsonPath("$.currency", is("KWD")));
    }

    @Test
    void wallet_overHttp() throws Exception {
        mockMvc.perform(get("/wallet").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", notNullValue()))
                .andExpect(jsonPath("$.currency", is("KWD")));
    }

    @Test
    void invoice_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/bookings/" + bookingId + "/invoice"))
                .andExpect(status().is4xxClientError());
    }
}
