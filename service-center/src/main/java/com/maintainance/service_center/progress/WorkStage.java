package com.maintainance.service_center.progress;

public enum WorkStage {
    RECEIVED,
    DIAGNOSING,
    QUOTE_READY,
    QUOTE_APPROVED,
    QUOTE_REJECTED,
    PARTS_ORDERED,
    PARTS_RECEIVED,
    WORK_IN_PROGRESS,
    QUALITY_CHECK,
    READY_FOR_PICKUP,
    PICKED_UP
}