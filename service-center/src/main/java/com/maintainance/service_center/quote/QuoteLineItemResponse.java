package com.maintainance.service_center.quote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteLineItemResponse {

    private String description;

    private String descriptionAr;

    private BigDecimal partsCost;

    private BigDecimal laborCost;

    // Spec 022 — discriminator. Null on legacy rows; treated as a regular user line.
    private QuoteLineItemKind kind;

    // Spec 022 — UI hints derived from kind. DIAGNOSTIC_FEE is non-editable, non-removable.
    private Boolean editable;
    private Boolean removable;

    // Spec 022 — i18n key the frontend resolves for system-generated lines. Null on user lines.
    private String descriptionKey;

    // Spec 025 — catalogued part ref (null = labor/legacy line), units consumed, and ad-hoc flag.
    private Long partId;
    private Integer quantity;
    private boolean adHoc;

    public static QuoteLineItemResponse from(QuoteLineItem item) {
        boolean systemLine = item.getKind() == QuoteLineItemKind.DIAGNOSTIC_FEE;
        return QuoteLineItemResponse.builder()
                .description(item.getDescription())
                .descriptionAr(item.getDescriptionAr())
                .partsCost(item.getPartsCost())
                .laborCost(item.getLaborCost())
                .kind(item.getKind())
                .editable(!systemLine)
                .removable(!systemLine)
                .descriptionKey(systemLine ? "quote.diagnosticFee.label" : null)
                .partId(item.getPartId())
                .quantity(item.getQuantity())
                .adHoc(item.isAdHoc())
                .build();
    }
}
