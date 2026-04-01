package com.maintainance.service_center.favorite;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFavoriteRequest {

    @NotNull(message = "Center ID is required")
    private Long centerId;

    private String notes;
}
