package com.maintainance.service_center.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteLineItem {

    @Column(nullable = false)
    private String description;

    private String descriptionAr;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal partsCost;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal laborCost;
}
