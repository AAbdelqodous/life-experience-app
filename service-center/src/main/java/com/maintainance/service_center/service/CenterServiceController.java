package com.maintainance.service_center.service;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("centers/my/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CENTER_OWNER')")
@Tag(name = "Center Services")
public class CenterServiceController {

    private final ServiceManagementService managementService;

    @GetMapping
    public ResponseEntity<List<CenterServiceResponse>> list(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(managementService.listForOwner(caller));
    }

    @PostMapping
    public ResponseEntity<CenterServiceResponse> create(
            @RequestBody @Valid CreateCenterServiceRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.create(request, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CenterServiceResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCenterServiceRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(managementService.update(id, request, caller));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        managementService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }
}
