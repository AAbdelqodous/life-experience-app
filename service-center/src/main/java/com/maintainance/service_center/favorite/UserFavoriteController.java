package com.maintainance.service_center.favorite;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites")
public class UserFavoriteController {

    private final UserFavoriteService service;

    @GetMapping("/my")
    public ResponseEntity<List<UserFavoriteResponse>> getMyFavorites(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.findByUser(caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserFavoriteResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.findById(id, caller));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserFavoriteResponse> create(
            @RequestBody @Valid UserFavoriteRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserFavoriteResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UserFavoriteRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.update(id, request, caller));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        service.delete(id, caller);
        return ResponseEntity.noContent().build();
    }
}
