package com.maintainance.service_center.reroute;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingService;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.department.DepartmentRepository;
import com.maintainance.service_center.quote.BookingQuoteResponse;
import com.maintainance.service_center.quote.BookingQuoteService;
import com.maintainance.service_center.quote.CreateQuoteRequest;
import com.maintainance.service_center.quote.QuoteLineItemKind;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 022 SC-DR-5: diagnostic fee rate changes by an owner MUST NOT retroactively alter
 * any already-claimed booking's owed amount. Captures the snapshot at claim time and
 * verifies the quote uses the snapshot, not the current fee rate.
 */
@SpringBootTest
@ActiveProfiles("dev")
class DiagnosticFeeSnapshotTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingService bookingService;
    @Autowired BookingQuoteService quoteService;
    @Autowired TransactionTemplate transactionTemplate;

    private MaintenanceCenter center;
    private Department diagnosticDept;
    private User owner;
    private User customer;
    private User tech;
    private CenterMembership techMembership;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            Role staffRole = roleRepository.findByName("ROLE_STAFF")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STAFF").build()));

            owner = userRepository.save(User.builder()
                    .firstname("Dfs").lastname("Owner")
                    .email("dfs-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.OWNER)
                    .roles(List.of(ownerRole))
                    .build());

            customer = userRepository.save(User.builder()
                    .firstname("Dfs").lastname("Customer")
                    .email("dfs-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER)
                    .build());

            tech = userRepository.save(User.builder()
                    .firstname("Dfs").lastname("Tech")
                    .email("dfs-tech-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234"))
                    .enabled(true).accountLocked(false)
                    .userType(UserType.STAFF)
                    .roles(List.of(staffRole))
                    .build());

            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("ث " + suffix).nameEn("Dfs Center " + suffix)
                    .email("dfs-c-" + suffix + "@test.local")
                    .phone("+96555555" + suffix.substring(0, 3))
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

            diagnosticDept = departmentRepository.save(Department.builder()
                    .center(center)
                    .nameAr("تشخيص " + suffix).nameEn("Diagnostic " + suffix)
                    .displayOrder(0).isActive(true)
                    .categories(new ArrayList<>())
                    .isDiagnostic(true)
                    .diagnosticFeeAmount(new BigDecimal("5.000"))
                    .build());

            techMembership = membershipRepository.save(CenterMembership.builder()
                    .user(tech).center(center)
                    .role(CenterRole.TECHNICIAN).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now())
                    .departments(new ArrayList<>(List.of(diagnosticDept)))
                    .build());
        });
    }

    @Test
    void quoteLineItem_usesSnapshot_notCurrentDeptRate() {
        // Create a diagnostic booking, simulate claim (sets the snapshot to 5.000).
        Long bookingId = transactionTemplate.execute(s -> {
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("DFS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(11, 0))
                    .bookingStatus(BookingStatus.CONFIRMED)
                    .department(diagnosticDept)
                    .passedThroughDiagnostic(true)
                    .build());
            return b.getId();
        });
        bookingService.claim(bookingId, tech);

        // Owner raises the fee to 7.000 after the booking has been claimed.
        transactionTemplate.executeWithoutResult(s -> {
            Department d = departmentRepository.findById(diagnosticDept.getId()).orElseThrow();
            d.setDiagnosticFeeAmount(new BigDecimal("7.000"));
            departmentRepository.save(d);
        });

        // Build the quote — it MUST reflect the 5.000 snapshot, not the new 7.000.
        CreateQuoteRequest req = new CreateQuoteRequest();
        QuoteLineItemRequest userLine = new QuoteLineItemRequest();
        userLine.setDescription("Parts");
        userLine.setPartsCost(new BigDecimal("10.000"));
        userLine.setLaborCost(new BigDecimal("2.000"));
        req.setLineItems(List.of(userLine));

        BookingQuoteResponse quote = quoteService.createQuote(owner, bookingId, req);

        // First line is the system-injected DIAGNOSTIC_FEE row.
        assertThat(quote.getLineItems()).hasSize(2);
        assertThat(quote.getLineItems().get(0).getKind()).isEqualTo(QuoteLineItemKind.DIAGNOSTIC_FEE);
        assertThat(quote.getLineItems().get(0).getEditable()).isFalse();
        assertThat(quote.getLineItems().get(0).getRemovable()).isFalse();
        assertThat(quote.getLineItems().get(0).getLaborCost()).isEqualByComparingTo("5.000");
        assertThat(quote.getLineItems().get(0).getDescriptionKey()).isEqualTo("quote.diagnosticFee.label");

        // Subtotal includes the 5.000 fee + 12.000 user line = 17.000.
        assertThat(quote.getSubtotal()).isEqualByComparingTo("17.000");
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("17.000");
    }

    @Test
    void quoteWithoutPassedThroughDiagnostic_hasNoFeeLine() {
        // Booking did NOT pass through diagnostic.
        Long bookingId = transactionTemplate.execute(s -> {
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("DFS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer)
                    .center(center)
                    .bookingDate(LocalDate.now().plusDays(1))
                    .bookingTime(LocalTime.of(12, 0))
                    .bookingStatus(BookingStatus.CONFIRMED)
                    .department(diagnosticDept) // same dept but the flag on the booking is false
                    .passedThroughDiagnostic(false)
                    .build());
            return b.getId();
        });

        CreateQuoteRequest req = new CreateQuoteRequest();
        QuoteLineItemRequest userLine = new QuoteLineItemRequest();
        userLine.setDescription("Parts");
        userLine.setPartsCost(new BigDecimal("10.000"));
        userLine.setLaborCost(new BigDecimal("2.000"));
        req.setLineItems(List.of(userLine));

        BookingQuoteResponse quote = quoteService.createQuote(owner, bookingId, req);

        // No DIAGNOSTIC_FEE line; only the single user line.
        assertThat(quote.getLineItems()).hasSize(1);
        assertThat(quote.getLineItems().get(0).getKind()).isNull();
        assertThat(quote.getSubtotal()).isEqualByComparingTo("12.000");
    }
}
