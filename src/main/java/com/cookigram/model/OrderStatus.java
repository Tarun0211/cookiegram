package com.cookigram.model;

public enum OrderStatus {
    PLACED("Placed"),
    IN_PREPARATION("In Preparation"),
    READY_FOR_DELIVERY("Ready for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    OrderStatus(String displayName) { this.displayName = displayName; }
    
    public String getDisplayName() { return displayName; }
    
    // Get valid next status for workflow
    public OrderStatus getNextStatus() {
        switch (this) {
            case PLACED: return IN_PREPARATION;
            case IN_PREPARATION: return READY_FOR_DELIVERY;
            case READY_FOR_DELIVERY: return DELIVERED;
            default: return null;
        }
    }
    
    // Check if transition to target status is valid
    public boolean canTransitionTo(OrderStatus target) {
        if (this == CANCELLED || this == DELIVERED) return false;
        if (target == CANCELLED) return this == PLACED; // Only PLACED can be cancelled
        return target == getNextStatus();
    }
}
