package com.maintainance.service_center.address;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private String streetAr;
    private String streetEn;
    private String districtAr;
    private String districtEn;
    private String cityAr;
    private String cityEn;
    private String governorateAr;
    private String governorateEn;
    private String postalCode;
    private String buildingNumber;
    private String floor;
    private String landMark;
}
