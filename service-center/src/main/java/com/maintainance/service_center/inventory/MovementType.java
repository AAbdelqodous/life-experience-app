package com.maintainance.service_center.inventory;

/**
 * Spec 025 — a stock movement. On-hand is the running sum of movement quantities (R2).
 * RECEIVE/CONSUME_REVERSAL are positive, CONSUME is negative, ADJUST is a signed delta.
 */
public enum MovementType { RECEIVE, ADJUST, CONSUME, CONSUME_REVERSAL }
