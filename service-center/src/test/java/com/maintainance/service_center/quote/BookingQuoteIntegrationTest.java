package com.maintainance.service_center.quote;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec 009 quote backend behavior: totals, versioning, REVISED auto-flip, send-state machine.
 */
@SpringBootTest
@ActiveProfiles("dev")
class BookingQuoteIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingQuoteRepository quoteRepository;
    @Autowired BookingQuoteService quoteService;
    @Autowired TransactionTemplate transactionTemplate;

    private User owner;
    private User customer;
    private MaintenanceCenter center;
    private Long bookingId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));

            owner = userRepository.save(User.builder()
                    .firstname("Q").lastname("Owner")
                    .email("q-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER)
                    .roles(List.of(ownerRole))
                    .build());

            customer = userRepository.save(User.builder()
                    .firstname("Q").lastname("Customer")
                    .email("q-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER)
                    .build());

            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("اقتباس " + suffix).nameEn("Q Center " + suffix)
                    .email("q-c-" + suffix + "@test.local")
                    .phone("+96577777" + suffix.substring(0, 3))
                    .categories(new ArrayList<>())
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

            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(11, 0))
                    .bookingStatus(BookingStatus.CONFIRMED)
                    .build());
            bookingId = b.getId();
        });
    }

    @Test
    void createQuote_computesSubtotalAndTotal() {
        CreateQuoteRequest req = new CreateQuoteRequest();
        req.setLineItems(List.of(
                lineItem("Oil change", "تغيير الزيت", "5.000", "3.000"),
                lineItem("Air filter", null, "8.500", "2.000")));
        req.setDiscountAmount(new BigDecimal("1.500"));
        req.setDiscountReason("Loyal customer");

        BookingQuoteResponse created = quoteService.createQuote(owner, bookingId, req);

        assertThat(created.getVersion()).isEqualTo(1);
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        assertThat(created.getSubtotal()).isEqualByComparingTo("18.500");
        assertThat(created.getDiscountAmount()).isEqualByComparingTo("1.500");
        assertThat(created.getTaxAmount()).isEqualByComparingTo("0.000");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("17.000");
    }

    @Test
    void createQuote_discountExceedsSubtotal_rejected() {
        CreateQuoteRequest req = new CreateQuoteRequest();
        req.setLineItems(List.of(lineItem("Tiny repair", null, "2.000", "1.000")));
        req.setDiscountAmount(new BigDecimal("10.000"));

        assertThatThrownBy(() -> quoteService.createQuote(owner, bookingId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount");
    }

    @Test
    void newDraftVersion_flipsPriorNonTerminalToRevised() {
        // v1 DRAFT
        BookingQuoteResponse v1 = quoteService.createQuote(owner, bookingId, simpleRequest("5.000"));
        assertThat(v1.getVersion()).isEqualTo(1);
        assertThat(v1.getStatus()).isEqualTo("DRAFT");

        // v2 — v1 should flip to REVISED
        BookingQuoteResponse v2 = quoteService.createQuote(owner, bookingId, simpleRequest("7.000"));
        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v2.getStatus()).isEqualTo("DRAFT");

        BookingQuote priorReloaded = quoteRepository.findById(v1.getId()).orElseThrow();
        assertThat(priorReloaded.getStatus()).isEqualTo(QuoteStatus.REVISED);
    }

    @Test
    void revisionAfterSent_flipsSentToRevised() {
        BookingQuoteResponse v1 = quoteService.createQuote(owner, bookingId, simpleRequest("5.000"));
        BookingQuoteResponse sent = quoteService.sendQuote(owner, bookingId, v1.getId());
        assertThat(sent.getStatus()).isEqualTo("SENT");
        assertThat(sent.getSentAt()).isNotNull();

        // New revision after the customer sees the SENT quote.
        quoteService.createQuote(owner, bookingId, simpleRequest("6.500"));

        BookingQuote priorReloaded = quoteRepository.findById(v1.getId()).orElseThrow();
        assertThat(priorReloaded.getStatus()).isEqualTo(QuoteStatus.REVISED);
    }

    @Test
    void sendingNonDraftQuote_rejected() {
        BookingQuoteResponse v1 = quoteService.createQuote(owner, bookingId, simpleRequest("5.000"));
        quoteService.sendQuote(owner, bookingId, v1.getId());

        assertThatThrownBy(() -> quoteService.sendQuote(owner, bookingId, v1.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRAFT");
    }

    private CreateQuoteRequest simpleRequest(String partsCost) {
        CreateQuoteRequest req = new CreateQuoteRequest();
        req.setLineItems(List.of(lineItem("Generic item", null, partsCost, "1.000")));
        return req;
    }

    private QuoteLineItemRequest lineItem(String desc, String descAr, String parts, String labor) {
        QuoteLineItemRequest item = new QuoteLineItemRequest();
        item.setDescription(desc);
        item.setDescriptionAr(descAr);
        item.setPartsCost(new BigDecimal(parts));
        item.setLaborCost(new BigDecimal(labor));
        return item;
    }
}
