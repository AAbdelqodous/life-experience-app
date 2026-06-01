package com.maintainance.service_center.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Spec 008 — the pickup/arrival time window (distinct from the appointment slot). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PickupWindow {

    @Column(name = "pickup_window_date")
    private LocalDate date;

    /** HH:mm. */
    @Column(name = "pickup_window_start")
    private String startTime;

    @Column(name = "pickup_window_end")
    private String endTime;
}
