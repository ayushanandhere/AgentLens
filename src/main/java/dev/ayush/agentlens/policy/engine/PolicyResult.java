package dev.ayush.agentlens.policy.engine;

import java.util.Collections;
import java.util.Map;

public record PolicyResult(boolean violated, Map<String, Object> details) {

    public static PolicyResult passed() {
        return new PolicyResult(false, Collections.emptyMap());
    }

    public static PolicyResult violated(Map<String, Object> details) {
        return new PolicyResult(true, details);
    }
}
