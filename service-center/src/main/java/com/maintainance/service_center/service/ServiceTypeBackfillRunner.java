package com.maintainance.service_center.service;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.category.ServiceCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One-shot idempotent backfill: populates booking.service_id and booking.category_id
 * from the legacy booking.service_type field.
 *
 * Activate by setting migration.backfill-booking-service=true in application-dev.yml,
 * restart once, then set it back to false.
 */
@Component
@ConditionalOnProperty(name = "migration.backfill-booking-service", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ServiceTypeBackfillRunner implements CommandLineRunner {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final CenterServiceRepository centerServiceRepository;

    private static final Map<ServiceType, String> SERVICE_TYPE_TO_CODE = Map.of(
            ServiceType.REPAIR,        "REPAIR",
            ServiceType.MAINTENANCE,   "MAINTENANCE",
            ServiceType.INSTALLATION,  "INSTALLATION",
            ServiceType.WARRANTY,      "WARRANTY",
            ServiceType.INSPECTION,    "INSPECTION"
            // CONSULTATION, EMERGENCY, OTHER — no catalog equivalent; stay null (legacy)
    );

    @Override
    @Transactional
    public void run(String... args) {
        List<Booking> unmigrated = bookingRepository.findAllUnmigratedBookings();
        log.info("Booking backfill: {} unmigrated booking(s) found", unmigrated.size());

        int migrated = 0;
        int skipped = 0;

        for (Booking booking : unmigrated) {
            String code = SERVICE_TYPE_TO_CODE.get(booking.getServiceType());
            if (code == null) {
                skipped++;
                continue;
            }

            Optional<Service> serviceOpt = serviceRepository.findByCode(code);
            if (serviceOpt.isEmpty()) {
                log.warn("Backfill: no Service entity found for code={}, skipping booking id={}",
                        code, booking.getId());
                skipped++;
                continue;
            }

            Service service = serviceOpt.get();
            booking.setService(service);

            // Derive category: find the first active CenterService for this center+service pair
            ServiceCategory category = centerServiceRepository
                    .findByCenterIdAndIsActiveTrueOrderByCategoryIdAscServiceIdAsc(
                            booking.getCenter().getId())
                    .stream()
                    .filter(cs -> cs.getService().getId().equals(service.getId()))
                    .map(CenterService::getCategory)
                    .findFirst()
                    .orElse(null);

            booking.setCategory(category);
            migrated++;
        }

        log.info("Booking backfill complete: {} migrated, {} skipped (unmapped serviceType)", migrated, skipped);
    }
}
