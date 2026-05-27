package com.maintainance.service_center.progress;

import java.util.Set;

/**
 * Repair-work stages for a booking. Transitions are enforced via {@link #canTransitionTo}.
 * The list MUST match the mobile repo's {@code types/workProgress.ts WORK_STAGES} — both sides
 * of the wire need to agree on which next-stages are valid from any current stage.
 * <p>Spec 009 FR-002: invalid transitions MUST be rejected by the backend, not just the UI.
 */
public enum WorkStage {
    RECEIVED         (Set.of("DIAGNOSING")),
    DIAGNOSING       (Set.of("QUOTE_READY")),
    QUOTE_READY      (Set.of("QUOTE_APPROVED", "QUOTE_REJECTED")),
    QUOTE_APPROVED   (Set.of("PARTS_ORDERED", "WORK_IN_PROGRESS")),
    QUOTE_REJECTED   (Set.of("PARTS_ORDERED", "WORK_IN_PROGRESS")),
    PARTS_ORDERED    (Set.of("PARTS_RECEIVED")),
    PARTS_RECEIVED   (Set.of("WORK_IN_PROGRESS")),
    WORK_IN_PROGRESS (Set.of("QUALITY_CHECK")),
    QUALITY_CHECK    (Set.of("READY_FOR_PICKUP", "WORK_IN_PROGRESS")),
    READY_FOR_PICKUP (Set.of("PICKED_UP")),
    PICKED_UP        (Set.of());

    private final Set<String> nextStageNames;

    WorkStage(Set<String> nextStageNames) {
        this.nextStageNames = nextStageNames;
    }

    public boolean canTransitionTo(WorkStage next) {
        return next != null && nextStageNames.contains(next.name());
    }

    public Set<String> getNextStageNames() {
        return nextStageNames;
    }
}
