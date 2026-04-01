package com.maintainance.service_center.search;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @Size(max = 500, message = "Search query must not exceed 500 characters")
    private String query;

    private Long categoryId;

    @Min(value = 0, message = "Minimum rating cannot be negative")
    @Max(value = 5, message = "Maximum rating cannot exceed 5")
    private Double minRating;

    @Min(value = 0, message = "Maximum distance cannot be negative")
    @Max(value = 100, message = "Maximum distance cannot exceed 100 km")
    private Double maxDistance;

    private String priceRange;

    private Boolean verifiedOnly;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String city;
}
