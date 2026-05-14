package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.handler.BusinessException;
import com.maintainance.service_center.service.CenterService;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CenterServicePricingServiceTest {

    @Mock private CenterServicePricingRepository pricingRepository;
    @Mock private MaintenanceCenterRepository centerRepository;
    @Mock private CenterMembershipRepository membershipRepository;

    @InjectMocks private CenterServicePricingService service;

    // ── Test 1: CAR center (all types allowed) + REPAIR → no exception ───────

    @Test
    void createPricing_allowedType_succeeds() {
        CenterService carCs = mockActiveService(new HashSet<>(Set.of(ServiceType.values())));
        MaintenanceCenter center = mockCenter(List.of(carCs));
        when(center.getId()).thenReturn(1L);
        when(centerRepository.findFirstByOwnerId(any())).thenReturn(Optional.of(center));
        when(pricingRepository.existsByCenterIdAndServiceTypeAndServiceNameEn(any(), any(), any()))
                .thenReturn(false);
        when(pricingRepository.save(any())).thenReturn(savedEntity(ServiceType.REPAIR));

        assertThatCode(() -> service.createPricing(mock(User.class), repairRequest()))
                .doesNotThrowAnyException();
    }

    // ── Test 2: RESTAURANT center (CONSULTATION+OTHER only) + REPAIR → reject ─

    @Test
    void createPricing_disallowedType_throwsBusinessException() {
        CenterService restaurantCs = mockActiveService(Set.of(ServiceType.CONSULTATION, ServiceType.OTHER));
        MaintenanceCenter center = mockCenter(List.of(restaurantCs));
        when(centerRepository.findFirstByOwnerId(any())).thenReturn(Optional.of(center));

        assertThatThrownBy(() -> service.createPricing(mock(User.class), repairRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(BusinessErrorCodes.SERVICE_TYPE_NOT_ALLOWED_FOR_CENTER));
    }

    // ── Test 3: [RESTAURANT, CAR] center → REPAIR allowed via flatMap union ──

    @Test
    void createPricing_multiCategoryCenter_unionAllowsType() {
        // RESTAURANT alone would reject REPAIR; CAR adds it — flatMap union must pass
        CenterService restaurantCs = mockActiveService(Set.of(ServiceType.CONSULTATION, ServiceType.OTHER));
        CenterService carCs = mockActiveService(new HashSet<>(Set.of(ServiceType.values())));
        MaintenanceCenter center = mockCenter(List.of(restaurantCs, carCs));
        when(center.getId()).thenReturn(1L);
        when(centerRepository.findFirstByOwnerId(any())).thenReturn(Optional.of(center));
        when(pricingRepository.existsByCenterIdAndServiceTypeAndServiceNameEn(any(), any(), any()))
                .thenReturn(false);
        when(pricingRepository.save(any())).thenReturn(savedEntity(ServiceType.REPAIR));

        assertThatCode(() -> service.createPricing(mock(User.class), repairRequest()))
                .doesNotThrowAnyException();
    }

    // ── Test 4: update path also enforces the check ──────────────────────────

    @Test
    void updatePricing_disallowedType_throwsBusinessException() {
        CenterService restaurantCs = mockActiveService(Set.of(ServiceType.CONSULTATION, ServiceType.OTHER));
        MaintenanceCenter center = mockCenter(List.of(restaurantCs));
        when(centerRepository.findFirstByOwnerId(any())).thenReturn(Optional.of(center));

        assertThatThrownBy(() -> service.updatePricing(mock(User.class), 1L, repairRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(BusinessErrorCodes.SERVICE_TYPE_NOT_ALLOWED_FOR_CENTER));
    }

    // ── Test 6: no registered services at all → BusinessException (no NPE) ───

    @Test
    void createPricing_noCenterServices_throwsBusinessException() {
        MaintenanceCenter center = mockCenter(List.of());
        when(centerRepository.findFirstByOwnerId(any())).thenReturn(Optional.of(center));

        assertThatThrownBy(() -> service.createPricing(mock(User.class), repairRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(BusinessErrorCodes.SERVICE_TYPE_NOT_ALLOWED_FOR_CENTER));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CenterServicePricingRequest repairRequest() {
        CenterServicePricingRequest req = new CenterServicePricingRequest();
        req.setServiceType(ServiceType.REPAIR);
        req.setServiceNameAr("إصلاح");
        req.setServiceNameEn("Repair");
        req.setMinPrice(new BigDecimal("5.000"));
        req.setMaxPrice(new BigDecimal("50.000"));
        return req;
    }

    private CenterService mockActiveService(Set<ServiceType> allowedTypes) {
        ServiceCategory cat = mock(ServiceCategory.class);
        when(cat.getAllowedServiceTypes()).thenReturn(new HashSet<>(allowedTypes));
        CenterService cs = mock(CenterService.class);
        when(cs.getIsActive()).thenReturn(true);
        when(cs.getCategory()).thenReturn(cat);
        return cs;
    }

    private MaintenanceCenter mockCenter(List<CenterService> centerServices) {
        MaintenanceCenter center = mock(MaintenanceCenter.class);
        when(center.getCenterServices()).thenReturn(centerServices);
        return center;
    }

    private CenterServicePricing savedEntity(ServiceType type) {
        return CenterServicePricing.builder()
                .serviceType(type)
                .serviceNameAr("إصلاح")
                .serviceNameEn("Repair")
                .minPrice(new BigDecimal("5.000"))
                .maxPrice(new BigDecimal("50.000"))
                .build();
    }
}
