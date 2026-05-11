package com.maintainance.service_center.config;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.service.Service;
import com.maintainance.service_center.service.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class ServiceCatalogSeeder implements ApplicationRunner {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedServiceCatalog();
        seedCategoryServiceLinks();
    }

    private void seedServiceCatalog() {
        List<ServiceDefinition> catalog = List.of(
                new ServiceDefinition("REPAIR",       "إصلاح",       "Repair"),
                new ServiceDefinition("MAINTENANCE",  "صيانة",       "Maintenance"),
                new ServiceDefinition("INSTALLATION", "تركيب",       "Installation"),
                new ServiceDefinition("WARRANTY",     "خدمة الضمان", "Warranty"),
                new ServiceDefinition("INSPECTION",   "فحص",         "Inspection"),
                new ServiceDefinition("BUYING",       "شراء",        "Buying"),
                new ServiceDefinition("SELLING",      "بيع",         "Selling")
        );

        int seeded = 0;
        for (ServiceDefinition def : catalog) {
            if (!serviceRepository.existsByCode(def.code())) {
                serviceRepository.save(Service.builder()
                        .code(def.code())
                        .nameAr(def.nameAr())
                        .nameEn(def.nameEn())
                        .isActive(true)
                        .build());
                seeded++;
            }
        }

        if (seeded > 0) {
            log.info("Seeded {} service catalog entries", seeded);
        }
    }

    private void seedCategoryServiceLinks() {
        Map<String, List<String>> mapping = Map.of(
                "CAR",            List.of("REPAIR", "MAINTENANCE", "INSTALLATION", "WARRANTY", "INSPECTION", "BUYING", "SELLING"),
                "ELECTRONICS",    List.of("REPAIR", "WARRANTY", "INSPECTION", "BUYING", "SELLING"),
                "HOME_APPLIANCE", List.of("REPAIR", "MAINTENANCE", "INSTALLATION", "WARRANTY", "INSPECTION")
        );

        int linked = 0;
        for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
            Optional<ServiceCategory> catOpt = categoryRepository.findByCode(entry.getKey());
            if (catOpt.isEmpty()) {
                log.warn("Category {} not found — skipping category_services seed for it", entry.getKey());
                continue;
            }
            ServiceCategory category = catOpt.get();

            for (String serviceCode : entry.getValue()) {
                Optional<Service> svcOpt = serviceRepository.findByCode(serviceCode);
                if (svcOpt.isEmpty()) continue;
                Service svc = svcOpt.get();

                boolean alreadyLinked = category.getServices().stream()
                        .anyMatch(s -> s.getCode().equals(serviceCode));
                if (!alreadyLinked) {
                    category.getServices().add(svc);
                    linked++;
                }
            }
            categoryRepository.save(category);
        }

        if (linked > 0) {
            log.info("Linked {} category-service pairs", linked);
        }
    }

    private record ServiceDefinition(String code, String nameAr, String nameEn) {}
}
