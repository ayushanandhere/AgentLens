package dev.ayush.agentlens.common.model;

public final class TraceStatus {

    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String BLOCKED = "BLOCKED";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";

    private TraceStatus() {
    }
}
