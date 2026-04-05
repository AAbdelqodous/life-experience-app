package com.maintainance.service_center.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Double rating;
    private String comment;
    private Long centerId;
    private String centerNameAr;
    private String centerNameEn;
    private String userFirstname;
    private String userLastname;
    private String ownerReply; // maps from Review.centerResponse
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
