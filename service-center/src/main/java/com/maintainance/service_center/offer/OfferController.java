package com.maintainance.service_center.offer;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("centers/my/offers")
@RequiredArgsConstructor
@Tag(name = "Offers")
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public List<OfferResponse> getMyOffers(
            @RequestParam(required = false) OfferStatus status,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return offerService.getMyOffers(user, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse createOffer(
            @RequestBody @Valid OfferRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return offerService.createOffer(user, request);
    }

    @GetMapping("/{id}")
    public OfferResponse getOffer(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return offerService.getOffer(user, id);
    }

    @PutMapping("/{id}")
    public OfferResponse updateOffer(
            @PathVariable Long id,
            @RequestBody @Valid OfferRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return offerService.updateOffer(user, id, request);
    }

    @PutMapping("/{id}/cancel")
    public OfferResponse cancelOffer(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return offerService.cancelOffer(user, id);
    }
}
