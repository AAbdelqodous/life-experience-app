package com.maintainance.service_center.booking;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookingQueueResponse {
    private List<BookingResponse> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean first;
    private boolean last;
    private boolean noDepartmentMembership;
}
