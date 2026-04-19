package com.maintainance.service_center.config;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private final ServiceCategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            seedCategories();
        }
    }

    private void seedCategories() {
        List<ServiceCategory> categories = List.of(
                ServiceCategory.builder()
                        .code("CAR")
                        .nameAr("سيارات")
                        .nameEn("Cars")
                        .descriptionAr("خدمات صيانة وإصلاح السيارات")
                        .descriptionEn("Car maintenance and repair services")
                        .displayOrder(1)
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .code("ELECTRONICS")
                        .nameAr("إلكترونيات")
                        .nameEn("Electronics")
                        .descriptionAr("خدمات صيانة الأجهزة الإلكترونية")
                        .descriptionEn("Electronics repair and maintenance services")
                        .displayOrder(2)
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .code("HOME_APPLIANCE")
                        .nameAr("الأجهزة المنزلية")
                        .nameEn("Home Appliances")
                        .descriptionAr("خدمات صيانة الأجهزة المنزلية")
                        .descriptionEn("Home appliance repair and maintenance services")
                        .displayOrder(3)
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .code("RESTAURANT")
                        .nameAr("مطاعم")
                        .nameEn("Restaurants")
                        .descriptionAr("خدمات المطاعم والكافيهات")
                        .descriptionEn("Restaurant and cafe services")
                        .displayOrder(4)
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .code("HOTEL")
                        .nameAr("فنادق")
                        .nameEn("Hotels")
                        .descriptionAr("خدمات الفنادق والإقامة")
                        .descriptionEn("Hotel and accommodation services")
                        .displayOrder(5)
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .code("OTHER")
                        .nameAr("أخرى")
                        .nameEn("Other")
                        .descriptionAr("خدمات أخرى")
                        .descriptionEn("Other services")
                        .displayOrder(6)
                        .isActive(true)
                        .build()
        );

        categoryRepository.saveAll(categories);
        log.info("Seeded {} service categories", categories.size());
    }
}
