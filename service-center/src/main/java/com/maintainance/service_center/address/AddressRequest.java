package com.maintainance.service_center.address;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
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
