package com.maintainance.service_center.fulfillment;

import com.maintainance.service_center.booking.AddressLabel;
import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.booking.FulfillmentMode;
import com.maintainance.service_center.booking.ServiceAddress;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.quote.BookingQuoteRepository;
import com.maintainance.service_center.payment.PaymentService;
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

/** Spec 008 — fulfillment: capability, fee computation, saved addresses, logistics, invoice line. */
@SpringBootTest
@ActiveProfiles("dev")
class FulfillmentIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingQuoteRepository quoteRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired FulfillmentService fulfillmentService;
    @Autowired PaymentService paymentService;
    @Autowired TransactionTemplate tx;

    private User customer;
    private User owner;
    private MaintenanceCenter center;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            customer = userRepository.save(User.builder()
                    .firstname("Ful").lastname("Cust").email("ful-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER).build());
            owner = userRepository.save(User.builder()
                    .firstname("Ful").lastname("Owner").email("ful-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.OWNER).roles(List.of(ownerRole)).build());
            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز " + suffix).nameEn("Ful Center " + suffix)
                    .email("ful-c-" + suffix + "@test.local").phone("+96555222" + suffix.substring(0, 3))
                    .categories(new ArrayList<>()).workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>()).imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>()).owner(owner)
                    .latitude(29.333).longitude(48.0)
                    .isVerified(false).isActive(true).enabled(true).build());
            membershipRepository.save(CenterMembership.builder()
                    .user(owner).center(center)
                    .role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now()).build());
        });
    }

    private ServiceAddress addr(double lat, double lng) {
        return new ServiceAddress(AddressLabel.HOME, "Hawalli", "Block 3", lat, lng, "Villa 5");
    }

    @Test
    void capability_servesDefaultModesAndFees() {
        CapabilityResponse cap = fulfillmentService.getCapability(center.getId(), 42L);
        assertThat(cap.supportedModes()).containsExactly("DROP_OFF", "PICKUP_DELIVERY", "AT_HOME");
        assertThat(cap.feeByMode().get("AT_HOME").flatAmount()).isEqualByComparingTo("5.000");
        assertThat(cap.feeByMode().get("PICKUP_DELIVERY").type()).isEqualTo("PER_KM");
        assertThat(cap.centerLat()).isEqualTo(29.333);
    }

    @Test
    void savedAddresses_crud() {
        var created = fulfillmentService.createAddress(customer, addr(29.35, 48.05));
        assertThat(created.id()).isNotNull();
        assertThat(fulfillmentService.listAddresses(customer)).extracting(SavedAddressView::id).contains(created.id());
        var updated = fulfillmentService.updateAddress(customer, created.id(),
                new ServiceAddress(AddressLabel.WORK, "Capital", "Sharq", null, null, "Tower B"));
        assertThat(updated.label()).isEqualTo(AddressLabel.WORK);
        assertThat(updated.governorate()).isEqualTo("Capital");
        fulfillmentService.deleteAddress(customer, created.id());
        assertThat(fulfillmentService.listAddresses(customer)).isEmpty();
    }

    @Test
    void prepare_atHome_isFlatFee_withInitialLeg() {
        var r = fulfillmentService.prepare(center, customer, FulfillmentMode.AT_HOME, null, addr(29.35, 48.05));
        assertThat(r.fee()).isEqualByComparingTo("5.000");
        assertThat(r.logisticsState()).isEqualTo("TECH_ASSIGNED");
        assertThat(r.address().getGovernorate()).isEqualTo("Hawalli");
    }

    @Test
    void prepare_pickup_addsPerKmOnTopOfBase() {
        var r = fulfillmentService.prepare(center, customer, FulfillmentMode.PICKUP_DELIVERY, null, addr(29.40, 48.10));
        assertThat(r.fee()).isGreaterThan(new BigDecimal("3.000")); // base + distance
        assertThat(r.logisticsState()).isEqualTo("PICKUP_SCHEDULED");
    }

    @Test
    void prepare_dropOff_isFreeWithNoAddress() {
        var r = fulfillmentService.prepare(center, customer, FulfillmentMode.DROP_OFF, null, null);
        assertThat(r.fee()).isEqualByComparingTo("0");
        assertThat(r.address()).isNull();
        assertThat(r.logisticsState()).isNull();
    }

    @Test
    void logistics_readAndReChooseToDropOff() {
        Long bookingId = tx.execute(s -> bookingRepository.save(Booking.builder()
                .bookingNumber("FUL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer).center(center)
                .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                .bookingStatus(BookingStatus.PENDING)
                .fulfillmentMode(FulfillmentMode.AT_HOME)
                .fulfillmentFee(new BigDecimal("5.000"))
                .logisticsState("TECH_ASSIGNED")
                .serviceAddress(addr(29.35, 48.05))
                .build()).getId());

        var logistics = fulfillmentService.getLogistics(customer, bookingId);
        assertThat(logistics.mode()).isEqualTo("AT_HOME");
        assertThat(logistics.currentState()).isEqualTo("TECH_ASSIGNED");

        fulfillmentService.reChoose(customer, bookingId, "DROP_OFF");
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getFulfillmentFee()).isEqualByComparingTo("0");
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getFulfillmentMode()).isEqualTo(FulfillmentMode.DROP_OFF);
    }

    @Test
    void centerAdvancesLogistics_stepwiseAndForwardJump() {
        Long bookingId = tx.execute(s -> bookingRepository.save(Booking.builder()
                .bookingNumber("FUL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer).center(center)
                .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                .bookingStatus(BookingStatus.PENDING)
                .fulfillmentMode(FulfillmentMode.PICKUP_DELIVERY)
                .fulfillmentFee(new BigDecimal("3.500"))
                .logisticsState("PICKUP_SCHEDULED")
                .serviceAddress(addr(29.40, 48.10))
                .build()).getId());

        // The customer view exposes the full ordered legs for the timeline.
        var logistics = fulfillmentService.getLogistics(customer, bookingId);
        assertThat(logistics.legs()).startsWith("PICKUP_SCHEDULED").endsWith("DELIVERED");

        // Stepwise: no target → next leg.
        var stepped = fulfillmentService.advanceLogistics(owner, bookingId, null);
        assertThat(stepped.currentState()).isEqualTo("DRIVER_EN_ROUTE_PICKUP");

        // Forward jump to a named later leg.
        var jumped = fulfillmentService.advanceLogistics(owner, bookingId, "OUT_FOR_DELIVERY");
        assertThat(jumped.currentState()).isEqualTo("OUT_FOR_DELIVERY");

        // Backward moves are rejected.
        assertThatThrownBy(() -> fulfillmentService.advanceLogistics(owner, bookingId, "PICKED_UP"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void centerAuthoredCapability_overridesDefaults() {
        // Owner offers only pickup (at-home omitted), a custom service area, and custom pickup fees.
        fulfillmentService.updateCapability(owner, new UpdateCapabilityRequest(
                List.of("PICKUP_DELIVERY"),
                List.of("Ahmadi", "Jahra"),
                new BigDecimal("2.000"),
                new BigDecimal("0.500"),
                null));

        var cap = fulfillmentService.getCapability(center.getId(), null);
        // DROP_OFF is always forced in; AT_HOME was dropped.
        assertThat(cap.supportedModes()).containsExactly("DROP_OFF", "PICKUP_DELIVERY");
        assertThat(cap.serviceAreaGovernorates()).containsExactly("Ahmadi", "Jahra");
        assertThat(cap.feeByMode().get("PICKUP_DELIVERY").baseAmount()).isEqualByComparingTo("2.000");
        assertThat(cap.feeByMode()).doesNotContainKey("AT_HOME");

        // The authored base flows into fee computation (no coords → base only).
        var r = fulfillmentService.prepare(center, customer, FulfillmentMode.PICKUP_DELIVERY, null,
                new ServiceAddress(AddressLabel.HOME, "Ahmadi", null, null, null, null));
        assertThat(r.fee()).isEqualByComparingTo("2.000");
    }

    @Test
    void centerAdvanceLogistics_rejectsNonMemberAndDropOff() {
        // A drop-off booking has no logistics legs.
        Long dropOff = tx.execute(s -> bookingRepository.save(Booking.builder()
                .bookingNumber("FUL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer).center(center)
                .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                .bookingStatus(BookingStatus.PENDING)
                .fulfillmentMode(FulfillmentMode.DROP_OFF)
                .build()).getId());
        assertThatThrownBy(() -> fulfillmentService.advanceLogistics(owner, dropOff, null))
                .isInstanceOf(ResponseStatusException.class);

        // A non-member (the customer) cannot drive a center's logistics.
        Long atHome = tx.execute(s -> bookingRepository.save(Booking.builder()
                .bookingNumber("FUL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer).center(center)
                .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                .bookingStatus(BookingStatus.PENDING)
                .fulfillmentMode(FulfillmentMode.AT_HOME)
                .logisticsState("TECH_ASSIGNED")
                .serviceAddress(addr(29.35, 48.05))
                .build()).getId());
        assertThatThrownBy(() -> fulfillmentService.advanceLogistics(customer, atHome, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
