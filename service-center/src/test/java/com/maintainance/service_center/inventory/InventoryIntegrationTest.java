package com.maintainance.service_center.inventory;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.quote.BookingQuoteResponse;
import com.maintainance.service_center.quote.BookingQuoteService;
import com.maintainance.service_center.quote.CreateQuoteRequest;
import com.maintainance.service_center.quote.QuoteLineItemRequest;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Spec 025 — inventory: catalog, movements, low-stock, and quote-driven consume/reversal. */
@SpringBootTest
@ActiveProfiles("dev")
class InventoryIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PartRepository partRepository;
    @Autowired StockMovementRepository movementRepository;
    @Autowired InventoryService service;
    @Autowired BookingQuoteService quoteService;
    @Autowired TransactionTemplate tx;

    private User owner;
    private User customer;
    private MaintenanceCenter center;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            owner = userRepository.save(User.builder()
                    .firstname("Inv").lastname("Owner").email("inv-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.OWNER).roles(List.of(ownerRole)).build());
            customer = userRepository.save(User.builder()
                    .firstname("Inv").lastname("Cust").email("inv-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER).build());
            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز " + suffix).nameEn("Inv Center " + suffix)
                    .email("inv-c-" + suffix + "@test.local").phone("+96555111" + suffix.substring(0, 3))
                    .categories(new ArrayList<>()).workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>()).imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>()).owner(owner)
                    .isVerified(false).isActive(true).enabled(true).build());
            membershipRepository.save(CenterMembership.builder()
                    .user(owner).center(center).role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now()).build());
        });
    }

    private PartResponse newPart(String sku, int threshold) {
        return service.createPart(owner, new CreatePartRequest(
                "تيل فرامل", "Brake pads", sku, "Brakes", Unit.SET,
                new BigDecimal("8.000"), new BigDecimal("12.500"), "AutoParts Co", threshold));
    }

    private Long newBooking() {
        return tx.execute(s -> bookingRepository.save(Booking.builder()
                .bookingNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer).center(center)
                .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                .bookingStatus(BookingStatus.IN_PROGRESS).build()).getId());
    }

    @Test
    void catalog_create_duplicateSku_rejected() {
        PartResponse p = newPart("BP-001", 3);
        assertThat(p.onHand()).isZero();
        assertThat(p.isActive()).isTrue();
        assertThatThrownBy(() -> newPart("BP-001", 3)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void receive_then_adjust_tracksOnHand_andMovementsReconcile() {
        PartResponse p = newPart("BP-002", 3);
        assertThat(service.receiveStock(owner, p.id(), new ReceiveStockRequest(10, new BigDecimal("8.000"))).onHand())
                .isEqualTo(10);
        assertThat(service.adjustStock(owner, p.id(), new AdjustStockRequest(6, "Count correction")).onHand())
                .isEqualTo(6);
        // Movements: RECEIVE +10, ADJUST -4 → sum equals on-hand (SC-004).
        int sum = service.getMovements(owner, p.id()).stream().mapToInt(MovementResponse::quantity).sum();
        assertThat(sum).isEqualTo(6);
    }

    @Test
    void lowStock_surfacesAtOrBelowThreshold() {
        PartResponse p = newPart("BP-003", 3);
        service.receiveStock(owner, p.id(), new ReceiveStockRequest(10, null));
        assertThat(service.getLowStock(owner)).noneMatch(i -> i.partId().equals(p.id()));
        service.adjustStock(owner, p.id(), new AdjustStockRequest(2, "Used in shop"));
        assertThat(service.getLowStock(owner)).anyMatch(i -> i.partId().equals(p.id()) && i.onHand() == 2);
    }

    @Test
    void quoteSend_consumesPart_snapshotsPrice_andReverseRestoresStock() {
        PartResponse part = newPart("BP-004", 3);
        service.receiveStock(owner, part.id(), new ReceiveStockRequest(10, new BigDecimal("8.000")));
        Long bookingId = newBooking();

        // Quote with a catalogued part line, qty 2 → partsCost snapshot = 12.500 × 2 = 25.000 (R4).
        QuoteLineItemRequest line = new QuoteLineItemRequest();
        line.setDescription("Front brake pads");
        line.setDescriptionAr("تيل فرامل أمامي");
        line.setPartsCost(BigDecimal.ZERO);   // overridden by server snapshot
        line.setLaborCost(new BigDecimal("5.000"));
        line.setPartId(part.id());
        line.setQuantity(2);
        CreateQuoteRequest req = new CreateQuoteRequest();
        req.setLineItems(List.of(line));

        BookingQuoteResponse quote = quoteService.createQuote(owner, bookingId, req);
        assertThat(quote.getLineItems().get(0).getPartsCost()).isEqualByComparingTo("25.000");
        // DRAFT → no stock effect yet.
        assertThat(partRepository.findById(part.id()).orElseThrow().getOnHand()).isEqualTo(10);

        // Send commits the parts → atomic decrement + booking-linked CONSUME movement (R3).
        quoteService.sendQuote(owner, bookingId, quote.getId());
        assertThat(partRepository.findById(part.id()).orElseThrow().getOnHand()).isEqualTo(8);

        // Reversal returns the stock; a second reversal is a no-op (idempotent, R5).
        service.reverseConsumption(bookingId, owner);
        assertThat(partRepository.findById(part.id()).orElseThrow().getOnHand()).isEqualTo(10);
        service.reverseConsumption(bookingId, owner);
        assertThat(partRepository.findById(part.id()).orElseThrow().getOnHand()).isEqualTo(10);
    }
}
