package com.maintainance.service_center.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.handler.BusinessException;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.security.JwtService;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the HTTP layer converts BusinessException(306) → 400 with
 * the correct businessErrorCode in the response body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CenterServicePricingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private CenterServicePricingService pricingService;

    // ── Test 5: POST with disallowed type returns HTTP 400 + businessErrorCode ─

    @Test
    void post_disallowedServiceType_returns400WithBusinessErrorCode306() throws Exception {
        Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
        User testUser = userRepository.save(User.builder()
                .firstname("P").lastname("Test")
                .email("pricing-ctrl-" + UUID.randomUUID() + "@test.local")
                .password(passwordEncoder.encode("Test@1234"))
                .enabled(true).accountLocked(false)
                .userType(UserType.OWNER)
                .roles(List.of(ownerRole))
                .build());
        String token = jwtService.generateToken(new HashMap<>(), testUser);

        when(pricingService.createPricing(any(), any()))
                .thenThrow(new BusinessException(BusinessErrorCodes.SERVICE_TYPE_NOT_ALLOWED_FOR_CENTER));

        CenterServicePricingRequest req = new CenterServicePricingRequest();
        req.setServiceType(ServiceType.REPAIR);
        req.setServiceNameAr("إصلاح");
        req.setServiceNameEn("Repair");
        req.setMinPrice(new BigDecimal("5.000"));
        req.setMaxPrice(new BigDecimal("50.000"));

        mockMvc.perform(post("/centers/my/pricing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.businessErrorCode", is(306)));
    }
}
