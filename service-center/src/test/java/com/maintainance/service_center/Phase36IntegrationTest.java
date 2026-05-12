package com.maintainance.service_center;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.security.JwtService;
import com.maintainance.service_center.service.CenterServiceRepository;
import com.maintainance.service_center.service.ServiceRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase36IntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired ServiceCategoryRepository categoryRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired CenterServiceRepository centerServiceRepository;

    private String ownerToken;
    private String customerToken;
    private Long centerId;
    private Long carCategoryId;
    private Long electronicsCatId;
    private Long repairServiceId;
    private Long maintenanceServiceId;
    private Long createdCsId;

    // ── Setup ────────────────────────────────────────────────────────────────

    @BeforeAll
    void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User owner = userRepository.save(User.builder()
                .firstname("Test").lastname("Owner")
                .email("owner-" + suffix + "@test.local")
                .password(passwordEncoder.encode("Test@1234"))
                .enabled(true).accountLocked(false)
                .userType(UserType.OWNER)
                .roles(List.of(ownerRole))
                .build());
        ownerToken = jwtService.generateToken(
                new HashMap<>(Map.of("fullName", owner.fullName())), owner);

        User customer = userRepository.save(User.builder()
                .firstname("Test").lastname("Customer")
                .email("customer-" + suffix + "@test.local")
                .password(passwordEncoder.encode("Test@1234"))
                .enabled(true).accountLocked(false)
                .userType(UserType.CUSTOMER)
                .roles(List.of(userRole))
                .build());
        customerToken = jwtService.generateToken(new HashMap<>(), customer);

        MaintenanceCenter center = centerRepository.save(MaintenanceCenter.builder()
                .nameAr("مركز " + suffix)
                .nameEn("Test Center " + suffix)
                .email("center-" + suffix + "@test.local")
                .phone("+9651234" + suffix.substring(0, 4))
                .categories(new java.util.ArrayList<>())
                .workingDays(new java.util.ArrayList<>())
                .specializations(new java.util.ArrayList<>())
                .imageUrls(new java.util.ArrayList<>())
                .certifications(new java.util.ArrayList<>())
                .owner(owner)
                .isVerified(false)
                .isActive(true)
                .build());
        centerId = center.getId();

        membershipRepository.save(CenterMembership.builder()
                .user(owner).center(center)
                .role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                .activatedAt(LocalDateTime.now()).build());

        carCategoryId = categoryRepository.findByCode("CAR").orElseThrow().getId();
        electronicsCatId = categoryRepository.findByCode("ELECTRONICS").orElseThrow().getId();
        repairServiceId = serviceRepository.findByCode("REPAIR").orElseThrow().getId();
        maintenanceServiceId = serviceRepository.findByCode("MAINTENANCE").orElseThrow().getId();
    }

    // ── 1. Public catalog ─────────────────────────────────────────────────────

    @Test @Order(1)
    void getServices_returnsSevenActiveServices() throws Exception {
        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));
    }

    @Test @Order(2)
    void getCategories_returnsAtLeastThreeCategories() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test @Order(3)
    void getCategoryServices_forCar_returnsSevenServices() throws Exception {
        mockMvc.perform(get("/categories/{id}/services", carCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));
    }

    // ── 2. Owner happy-path CRUD ──────────────────────────────────────────────

    @Test @Order(4)
    void owner_createCenterService_returns201WithServiceAndCategoryInfo() throws Exception {
        Map<String, Object> req = Map.of(
                "categoryId", carCategoryId,
                "serviceId", repairServiceId,
                "minPrice", 5.000,
                "maxPrice", 25.000,
                "typicalDurationMinutes", 60
        );
        MvcResult result = mockMvc.perform(post("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.service.code", is("REPAIR")))
                .andExpect(jsonPath("$.category.code", is("CAR")))
                .andReturn();

        createdCsId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Test @Order(5)
    void owner_listCenterServices_includesCreatedEntry() throws Exception {
        mockMvc.perform(get("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test @Order(6)
    void owner_updateCenterService_returns200WithUpdatedPricing() throws Exception {
        mockMvc.perform(put("/centers/my/services/{id}", createdCsId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("minPrice", 10.000, "maxPrice", 50.000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minPrice", is(10.0)))
                .andExpect(jsonPath("$.maxPrice", is(50.0)));
    }

    // ── 3. Owner validation failures ──────────────────────────────────────────

    @Test @Order(7)
    void owner_createDuplicate_returns409Conflict() throws Exception {
        mockMvc.perform(post("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", carCategoryId, "serviceId", repairServiceId))))
                .andExpect(status().isConflict());
    }

    @Test @Order(8)
    void owner_createInvalidCategoryServicePair_isRejected() throws Exception {
        // ELECTRONICS does NOT offer MAINTENANCE in category_services — must be rejected
        mockMvc.perform(post("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", electronicsCatId, "serviceId", maintenanceServiceId))))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(9)
    void owner_createWithMaxPriceLessThanMin_isRejected() throws Exception {
        // Price coherence violation: maxPrice < minPrice must be rejected
        mockMvc.perform(post("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", carCategoryId, "serviceId", maintenanceServiceId,
                                "minPrice", 100.0, "maxPrice", 10.0))))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(10)
    void owner_createWithMinPriceAndNoMax_isRejected() throws Exception {
        // minPrice set without maxPrice must be rejected
        mockMvc.perform(post("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", carCategoryId, "serviceId", maintenanceServiceId,
                                "minPrice", 10.0))))
                .andExpect(status().is4xxClientError());
    }

    // ── 4. Customer drill-down ────────────────────────────────────────────────

    @Test @Order(11)
    void customer_getCenterCategories_includesCarCategory() throws Exception {
        mockMvc.perform(get("/centers/{id}/categories", centerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("CAR")));
    }

    @Test @Order(12)
    void customer_getCenterCategoryServices_returnsRepairUnderCar() throws Exception {
        mockMvc.perform(get("/centers/{id}/categories/{catId}/services", centerId, carCategoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].service.code", hasItem("REPAIR")));
    }

    @Test @Order(13)
    void unauthenticated_getCenterCategories_returns4xx() throws Exception {
        // Spring Security 6: unauthenticated → 401 or 403 depending on config
        mockMvc.perform(get("/centers/{id}/categories", centerId))
                .andExpect(status().is4xxClientError());
    }

    // ── 5. Soft delete ────────────────────────────────────────────────────────

    @Test @Order(14)
    void owner_deleteCenterService_softDeletesRow() throws Exception {
        // Add a second service (CAR/MAINTENANCE) to delete
        MvcResult created = mockMvc.perform(post("/centers/my/services")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", carCategoryId, "serviceId", maintenanceServiceId))))
                .andExpect(status().isCreated())
                .andReturn();

        Long toDeleteId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(delete("/centers/my/services/{id}", toDeleteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        assertThat(centerServiceRepository.findById(toDeleteId))
                .isPresent()
                .hasValueSatisfying(cs -> assertThat(cs.getIsActive()).isFalse());
    }

    // ── 6. Booking validation ─────────────────────────────────────────────────

    @Test @Order(15)
    void booking_withNewCategoryAndServiceId_populatesServiceSummary() throws Exception {
        HashMap<String, Object> req = bookingBase();
        req.put("categoryId", carCategoryId);
        req.put("serviceId", repairServiceId);

        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceSummary.code", is("REPAIR")))
                .andExpect(jsonPath("$.categorySummary.code", is("CAR")));
    }

    @Test @Order(16)
    void booking_withPairNotOfferedByCenter_isRejected() throws Exception {
        // Center does not offer ELECTRONICS/REPAIR — must be rejected
        HashMap<String, Object> req = bookingBase();
        req.put("categoryId", electronicsCatId);
        req.put("serviceId", repairServiceId);

        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test @Order(17)
    void booking_withLegacyServiceTypeOnly_returns201WithNullSummaries() throws Exception {
        HashMap<String, Object> req = bookingBase();
        req.put("serviceType", "REPAIR");

        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceType", is("REPAIR")))
                .andExpect(jsonPath("$.serviceSummary").doesNotExist());
    }

    @Test @Order(18)
    void booking_withNeitherServiceTypeNorNewFields_isRejected() throws Exception {
        // No serviceType, no categoryId+serviceId — must be rejected
        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingBase())))
                .andExpect(status().is4xxClientError());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HashMap<String, Object> bookingBase() {
        HashMap<String, Object> req = new HashMap<>();
        req.put("centerId", centerId);
        req.put("bookingDate", java.time.LocalDate.now().plusDays(1).toString());
        req.put("bookingTime", "10:00:00");
        req.put("customerPhone", "+96555500000");
        return req;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
