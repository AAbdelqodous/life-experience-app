package com.maintainance.service_center.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    // Spec 022 — discriminator for system-generated lines (DIAGNOSTIC_FEE) vs. user lines.
    // Null on legacy rows written before spec 022; consumers treat null as a user line.
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private QuoteLineItemKind kind;

    // Spec 025 — catalogued part reference. partId set + adHoc false → decrements stock on commit and
    // snapshots salePrice×quantity into partsCost (R4). adHoc true → one-off part, no stock effect.
    // partId null → pure labor/legacy line. quantity defaults to 1 for a part line.
    @Column(name = "part_id")
    private Long partId;

    private Integer quantity;

    @Column(name = "ad_hoc")
    private boolean adHoc;
}
