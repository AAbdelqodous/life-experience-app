package com.maintainance.service_center.favorite;

import com.maintainance.service_center.address.Address;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserFavoriteResponse {
    private Long id;
    private Integer userId;
    private String userName;
    private Long centerId;
    private String centerName;
    private String centerPhone;
    private Address centerAddress;
    private Double centerLatitude;
    private Double centerLongitude;
    private String notes;
    private LocalDateTime createdAt;
}
