package dev.ayush.agentlens.common.model;

public final class ViolationAction {

    public static final String BLOCKED = "BLOCKED";
    public static final String WARNED = "WARNED";
    public static final String LOGGED = "LOGGED";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED_OVERRIDE = "APPROVED_OVERRIDE";
    public static final String REJECTED = "REJECTED";

    private ViolationAction() {
    }
}
