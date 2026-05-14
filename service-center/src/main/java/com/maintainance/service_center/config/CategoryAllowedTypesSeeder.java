package com.maintainance.service_center.config;

import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
public class CategoryAllowedTypesSeeder implements ApplicationRunner {

    private final ServiceCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAllowedTypes();
    }

    private void seedAllowedTypes() {
        Set<ServiceType> all = Set.of(ServiceType.values());
        Set<ServiceType> hospitality = Set.of(ServiceType.CONSULTATION, ServiceType.OTHER);

        seed("CAR",            all);
        seed("ELECTRONICS",    all);
        seed("HOME_APPLIANCE", all);
        seed("RESTAURANT",     hospitality); // Minimal set — ServiceType enum needs domain-appropriate values for hospitality. See backlog.
        seed("HOTEL",          hospitality); // Minimal set — ServiceType enum needs domain-appropriate values for hospitality. See backlog.
        seed("OTHER",          all);
    }

    private void seed(String code, Set<ServiceType> types) {
        categoryRepository.findByCode(code).ifPresent(cat -> {
            if (cat.getAllowedServiceTypes().isEmpty()) {
                cat.getAllowedServiceTypes().addAll(types);
                categoryRepository.save(cat);
                log.info("Seeded {} allowed service types for category {}", types.size(), code);
            }
        });
    }
}
