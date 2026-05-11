package com.maintainance.service_center.service;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Service Catalog")
public class ServiceController {

    private final CatalogService catalogService;

    @GetMapping("/services")
    public ResponseEntity<List<ServiceResponse>> findAllActive() {
        return ResponseEntity.ok(catalogService.findAllActive());
    }

    @GetMapping("/categories/{id}/services")
    public ResponseEntity<List<ServiceResponse>> findByCategory(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.findByCategory(id));
    }
}
