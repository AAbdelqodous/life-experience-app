package com.maintainance.service_center.offer;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private static final int MAX_ACTIVE_SCHEDULED_OFFERS = 10;

    private final OfferRepository offerRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final CenterMembershipRepository membershipRepository;

    public OfferStatus computeStatus(CenterOffer offer) {
        if (offer.getCancelledAt() != null) return OfferStatus.CANCELLED;
        LocalDate today = LocalDate.now();
        if (offer.getStartDate().isAfter(today)) return OfferStatus.SCHEDULED;
        if (!offer.getEndDate().isBefore(today)) return OfferStatus.ACTIVE;
        return OfferStatus.EXPIRED;
    }

    private MaintenanceCenter resolveCenter(User user) {
        return centerRepository.findFirstByOwnerId(user.getId())
                .or(() -> membershipRepository
                        .findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                        .stream()
                        .findFirst()
                        .map(m -> m.getCenter()))
                .orElseThrow(() -> new EntityNotFoundException("No center found for this user"));
    }

    @Transactional
    public OfferResponse createOffer(User owner, OfferRequest request) {
        MaintenanceCenter center = resolveCenter(owner);
        validateDatesAndValue(request.getStartDate(), request.getEndDate(),
                request.getDiscountType(), request.getDiscountValue());

        long activeCount = offerRepository.countActiveOrScheduled(center.getId(), LocalDate.now());
        if (activeCount >= MAX_ACTIVE_SCHEDULED_OFFERS) {
            throw new IllegalStateException(
                    "You have reached the maximum of 10 active or scheduled offers");
        }

        CenterOffer offer = CenterOffer.builder()
                .center(center)
                .titleAr(request.getTitleAr())
                .titleEn(request.getTitleEn())
                .descriptionAr(request.getDescriptionAr())
                .descriptionEn(request.getDescriptionEn())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .applicableServiceTypes(request.getApplicableServiceTypes())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .maxRedemptions(request.getMaxRedemptions())
                .currentRedemptions(0)
                .cancelledAt(null)
                .build();

        CenterOffer saved = offerRepository.save(offer);
        log.info("Created offer id={} for center id={}", saved.getId(), center.getId());
        return OfferResponse.from(saved, computeStatus(saved));
    }

    public List<OfferResponse> getMyOffers(User owner, OfferStatus statusFilter) {
        MaintenanceCenter center = resolveCenter(owner);
        List<CenterOffer> all = offerRepository.findByCenterIdOrderByCreatedAtDesc(center.getId());
        return all.stream()
                .map(o -> OfferResponse.from(o, computeStatus(o)))
                .filter(r -> statusFilter == null || r.getStatus() == statusFilter)
                .collect(Collectors.toList());
    }

    public OfferResponse getOffer(User owner, Long offerId) {
        MaintenanceCenter center = resolveCenter(owner);
        CenterOffer offer = offerRepository.findByIdAndCenterId(offerId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));
        return OfferResponse.from(offer, computeStatus(offer));
    }

    @Transactional
    public OfferResponse updateOffer(User owner, Long offerId, OfferRequest request) {
        MaintenanceCenter center = resolveCenter(owner);
        CenterOffer offer = offerRepository.findByIdAndCenterId(offerId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        OfferStatus currentStatus = computeStatus(offer);
        if (currentStatus == OfferStatus.EXPIRED || currentStatus == OfferStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot edit an offer with status " + currentStatus);
        }

        if (currentStatus == OfferStatus.ACTIVE) {
            validateActiveOfferEdit(offer, request);
            offer.setTitleAr(request.getTitleAr());
            offer.setTitleEn(request.getTitleEn());
            offer.setDescriptionAr(request.getDescriptionAr());
            offer.setDescriptionEn(request.getDescriptionEn());
            offer.setEndDate(request.getEndDate());
        } else {
            validateDatesAndValue(request.getStartDate(), request.getEndDate(),
                    request.getDiscountType(), request.getDiscountValue());
            offer.setTitleAr(request.getTitleAr());
            offer.setTitleEn(request.getTitleEn());
            offer.setDescriptionAr(request.getDescriptionAr());
            offer.setDescriptionEn(request.getDescriptionEn());
            offer.setDiscountType(request.getDiscountType());
            offer.setDiscountValue(request.getDiscountValue());
            offer.setApplicableServiceTypes(request.getApplicableServiceTypes());
            offer.setStartDate(request.getStartDate());
            offer.setEndDate(request.getEndDate());
            offer.setMaxRedemptions(request.getMaxRedemptions());
        }

        CenterOffer saved = offerRepository.save(offer);
        return OfferResponse.from(saved, computeStatus(saved));
    }

    @Transactional
    public OfferResponse cancelOffer(User owner, Long offerId) {
        MaintenanceCenter center = resolveCenter(owner);
        CenterOffer offer = offerRepository.findByIdAndCenterId(offerId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        OfferStatus currentStatus = computeStatus(offer);
        if (currentStatus == OfferStatus.EXPIRED || currentStatus == OfferStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel an offer with status " + currentStatus);
        }

        offer.setCancelledAt(LocalDateTime.now());
        CenterOffer saved = offerRepository.save(offer);
        log.info("Cancelled offer id={}", offerId);
        return OfferResponse.from(saved, OfferStatus.CANCELLED);
    }

    private void validateDatesAndValue(LocalDate startDate, LocalDate endDate,
                                       DiscountType type, BigDecimal value) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (endDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("End date must be in the future");
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100");
        }
    }

    private void validateActiveOfferEdit(CenterOffer offer, OfferRequest request) {
        if (request.getEndDate().isBefore(offer.getEndDate())) {
            throw new IllegalArgumentException("End date can only be extended for active offers");
        }
        if (request.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("End date must be in the future");
        }
    }
}
