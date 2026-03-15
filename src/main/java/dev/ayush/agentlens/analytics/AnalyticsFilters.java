package dev.ayush.agentlens.analytics;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AnalyticsFilters {

    private final Instant startDate;
    private final Instant endDate;
    private final UUID agentId;
    private final String tenantId;
}
