package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitEvaluator implements PolicyEvaluator {

    private final StringRedisTemplate redisTemplate;

    @Override
    public PolicyType getType() {
        return PolicyType.RATE_LIMIT;
    }

    @Override
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        int maxRunsPerMinute = ((Number) config.get("max_runs_per_minute")).intValue();
        String agentId = context.getAgent().getId().toString();
        long windowSeconds = Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond();

        String key = "ratelimit:" + agentId + ":" + windowSeconds;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(120));

        if (count != null && count > maxRunsPerMinute) {
            return PolicyResult.violated(Map.of(
                    "limit_per_minute", maxRunsPerMinute,
                    "current_count", count,
                    "window", windowSeconds
            ));
        }
        return PolicyResult.passed();
    }
}
