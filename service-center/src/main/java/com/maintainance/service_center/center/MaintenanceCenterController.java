package com.maintainance.service_center.center;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("centers")
@RequiredArgsConstructor
@Tag(name = "Maintenance Centers")
public class MaintenanceCenterController {

    private final MaintenanceCenterService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MaintenanceCenterResponse> create(
            @RequestBody @Valid MaintenanceCenterRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, caller));
    }

    @GetMapping
    public ResponseEntity<Page<MaintenanceCenterSummaryResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceCenterResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/my")
    public ResponseEntity<MaintenanceCenterResponse> updateMy(
            @RequestBody @Valid MaintenanceCenterRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.updateMy(request, caller));
    }

    @PostMapping(value = "/my/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaintenanceCenterResponse> addImages(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.addImages(file, caller));
    }

    @GetMapping("/my/profile")
    public ResponseEntity<MaintenanceCenterResponse> getMyCenterProfile(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.getMyCenterProfile(caller));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<MaintenanceCenterSummaryResponse>> findMyCenters(
            @AuthenticationPrincipal User caller,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByOwner(caller, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MaintenanceCenterSummaryResponse>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.search(query, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<MaintenanceCenterSummaryResponse>> findByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByCategory(categoryId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceCenterResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid MaintenanceCenterRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.update(id, request, caller));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        service.deactivate(id, caller);
        return ResponseEntity.noContent().build();
    }
}
