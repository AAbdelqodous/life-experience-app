package com.maintainance.service_center.quote;

/**
 * Discriminator on {@link QuoteLineItem}.
 * <p>{@code PARTS} and {@code LABOR} are conceptual labels — the line item itself carries
 * both {@code partsCost} and {@code laborCost} columns, so a "PARTS" line typically has
 * {@code laborCost = 0} and vice versa. The discriminator is most meaningful for
 * {@code DIAGNOSTIC_FEE} (spec 022), which is system-generated and immutable.
 * <p>Legacy line items written before spec 022 may carry a null kind; consumers should
 * treat null as "user line item" (editable/removable).
 */
public enum QuoteLineItemKind {
    PARTS,
    LABOR,
    DIAGNOSTIC_FEE
}
