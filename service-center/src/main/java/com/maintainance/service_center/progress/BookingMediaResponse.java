package com.maintainance.service_center.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingMediaResponse {
    
    private Long id;
    private Long bookingId;
    private MediaType mediaType;
    private MediaCategory category;
    private String url;
    private String thumbnailUrl;
    private String caption;
    private String captionAr;
    private Boolean isVisibleToCustomer;
    private LocalDateTime createdAt;
}