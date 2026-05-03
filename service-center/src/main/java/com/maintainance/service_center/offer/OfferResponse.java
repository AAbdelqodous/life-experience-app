package com.maintainance.service_center.offer;

import com.maintainance.service_center.booking.ServiceType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OfferResponse {

    private Long id;
    private String titleAr;
    private String titleEn;
    private String descriptionAr;
    private String descriptionEn;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private List<ServiceType> applicableServiceTypes;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxRedemptions;
    private int currentRedemptions;
    private OfferStatus status;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    public static OfferResponse from(CenterOffer offer, OfferStatus status) {
        return OfferResponse.builder()
                .id(offer.getId())
                .titleAr(offer.getTitleAr())
                .titleEn(offer.getTitleEn())
                .descriptionAr(offer.getDescriptionAr())
                .descriptionEn(offer.getDescriptionEn())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .applicableServiceTypes(offer.getApplicableServiceTypes())
                .startDate(offer.getStartDate())
                .endDate(offer.getEndDate())
                .maxRedemptions(offer.getMaxRedemptions())
                .currentRedemptions(offer.getCurrentRedemptions())
                .status(status)
                .cancelledAt(offer.getCancelledAt())
                .createdAt(offer.getCreatedAt())
                .build();
    }
}
