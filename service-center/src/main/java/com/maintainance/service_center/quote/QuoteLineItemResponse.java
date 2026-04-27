package com.maintainance.service_center.quote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteLineItemResponse {

    private String description;

    private String descriptionAr;

    private BigDecimal partsCost;

    private BigDecimal laborCost;
}
