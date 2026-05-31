package com.maintainance.service_center.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Spec 008 — a service address for pickup / at-home fulfillment. An {@code @Embeddable} that doubles
 * as the inline request shape (Jackson-deserializable). Stored on the booking and on a SavedAddress.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAddress {

    @Enumerated(EnumType.STRING)
    @Column(name = "address_label")
    private AddressLabel label;

    private String governorate;

    private String area;

    @Column(name = "addr_lat")
    private Double lat;

    @Column(name = "addr_lng")
    private Double lng;

    @Column(name = "addr_note", length = 500)
    private String note;
}
