package com.maintainance.service_center.fulfillment;

import com.maintainance.service_center.booking.AddressLabel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Spec 008 — fulfillment capability + logistics view records (package-private). */
final class FulfillmentDtos {
    private FulfillmentDtos() {}
}

record FeeRuleView(String type, BigDecimal flatAmount, BigDecimal baseAmount, BigDecimal perKm) {}

record CapabilityResponse(
        Long centerId,
        Long serviceId,
        List<String> supportedModes,
        List<String> serviceAreaGovernorates,
        Map<String, FeeRuleView> feeByMode,
        Double centerLat,
        Double centerLng) {}

record LogisticsResponse(
        Long bookingId,
        String mode,
        String currentState,
        String etaText,
        boolean declined,
        String declineReason,
        List<String> legs,
        String updatedAt) {}

record SavedAddressView(
        Long id, AddressLabel label, String governorate, String area,
        Double lat, Double lng, String note) {}

record ReChooseRequest(String mode) {}

/** Center advances the logistics leg: omit targetState to step to the next leg, or jump forward to a named one. */
record AdvanceLogisticsRequest(String targetState) {}

/** Owner authors the center's fulfillment capability (modes / service area / pickup + at-home fees). */
record UpdateCapabilityRequest(
        List<String> supportedModes,
        List<String> serviceAreaGovernorates,
        BigDecimal pickupBase,
        BigDecimal pickupPerKm,
        BigDecimal atHomeFlat) {}
