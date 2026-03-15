package dev.ayush.agentlens.demo;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.trace.Trace;
import dev.ayush.agentlens.trace.TraceRepository;
import dev.ayush.agentlens.trace.TraceService;
import dev.ayush.agentlens.trace.dto.AddEventRequest;
import dev.ayush.agentlens.trace.dto.CompleteTraceRequest;
import dev.ayush.agentlens.trace.dto.StartTraceRequest;
import dev.ayush.agentlens.trace.dto.TraceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentlens.demo.enabled", havingValue = "true")
@Slf4j
public class SimulatedTraceGenerator {

    private static final List<String> TENANTS = List.of("tenant-acme", "tenant-zenith", "tenant-orbit");
    private static final List<String> NORMAL_FAILURES = List.of("Tool timeout", "Upstream API rejected request", "Partial data unavailable");
    private static final List<String> TRACE_FAILURES = List.of("LLM timeout", "Context window exceeded", "Rate limited by provider");

    private static final Map<String, List<String>> TOOLS = Map.of(
            "financial-analyst", List.of("searchFinancialData", "fetchReport", "queryDatabase", "calculateMetrics", "generateChart"),
            "code-reviewer", List.of("fetchPullRequest", "analyzeCode", "checkStyle", "runTests", "searchCodebase"),
            "customer-support", List.of("searchTickets", "fetchCustomerProfile", "queryKnowledgeBase", "sendNotification"),
            "data-pipeline", List.of("querySource", "validateSchema", "transformData", "writeOutput", "checkDataQuality")
    );

    private final AgentRepository agentRepository;
    private final TraceRepository traceRepository;
    private final TraceService traceService;
    private final AuditEventProducer auditEventProducer;
    private final AtomicLong sequence = new AtomicLong();

    @Scheduled(fixedRate = 3000)
    public void generateTrace() {
        List<Agent> agents = new ArrayList<>(agentRepository.findAllByNameIn(
                DemoAgentRunner.DEMO_AGENTS.stream().map(DemoAgentRunner.DemoAgentDefinition::name).toList()));
        agents.sort(Comparator.comparingInt(agent -> demoIndex(agent.getName())));
        if (agents.isEmpty()) {
            return;
        }

        long sequenceNumber = sequence.getAndIncrement();
        Agent agent = chooseAgent(agents, sequenceNumber);
        Scenario scenario = chooseScenario(sequenceNumber);
        String model = chooseModel(scenario, sequenceNumber);
        String prompt = buildPrompt(agent.getName(), scenario);

        try {
            TraceResponse trace = traceService.startTrace(new StartTraceRequest(
                    agent.getId(),
                    model,
                    prompt,
                    randomTemperature(),
                    randomElement(TENANTS),
                    UUID.randomUUID().toString(),
                    buildMetadata(agent.getName(), scenario)
            ));

            int eventCount = randomInt(2, 4);
            for (int i = 0; i < eventCount; i++) {
                traceService.addEvent(trace.getId(), buildEvent(agent.getName(), scenario, i == eventCount - 1));
            }

            if (scenario == Scenario.FAILED) {
                String failureReason = randomElement(TRACE_FAILURES);
                traceService.addEvent(trace.getId(), new AddEventRequest(
                        "LLM_CALL",
                        "generateResponse",
                        Map.of("model", model),
                        Map.of(),
                        "FAILURE",
                        (long) randomInt(300, 1200),
                        failureReason
                ));
                markTraceAsFailed(trace.getId(), agent.getId(), failureReason);
                return;
            }

            TokenProfile tokenProfile = tokensForScenario(scenario);
            CompleteTraceRequest completeRequest = new CompleteTraceRequest(
                    buildResponse(agent.getName(), scenario),
                    tokenProfile.inputTokens(),
                    tokenProfile.outputTokens(),
                    randomGroundingScore(),
                    (long) randomInt(100, 800)
            );
            TraceResponse completed = traceService.completeTrace(trace.getId(), completeRequest);
            log.info("Generated demo trace {} for agent {} with status {}", completed.getId(), agent.getName(), completed.getStatus());
        } catch (Exception ex) {
            log.warn("Skipping demo trace for agent {} due to {}", agent.getName(), ex.getMessage());
        }
    }

    private void markTraceAsFailed(UUID traceId, UUID agentId, String failureReason) {
        Trace trace = traceRepository.findById(traceId).orElseThrow();
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (trace.getMetadata() != null) {
            metadata.putAll(trace.getMetadata());
        }
        metadata.put("failure_reason", failureReason);

        trace.setStatus("FAILED");
        trace.setCompletedAt(LocalDateTime.now());
        trace.setLatencyMs(Duration.between(trace.getStartedAt(), trace.getCompletedAt()).toMillis());
        trace.setResponseText(failureReason);
        trace.setMetadata(metadata);
        trace.setPolicyResult(trace.getPolicyResult() != null ? trace.getPolicyResult() : "PASS");
        traceRepository.save(trace);

        auditEventProducer.publish("system:demo-agent", "TRACE_FAILED", "TRACE", trace.getId().toString(),
                Map.of("agent_id", agentId.toString(), "reason", failureReason));
        log.info("Marked demo trace {} as FAILED", trace.getId());
    }

    private AddEventRequest buildEvent(String agentName, Scenario scenario, boolean lastEvent) {
        String toolName;
        if (scenario == Scenario.TOOL_BLOCK && lastEvent) {
            toolName = ThreadLocalRandom.current().nextBoolean() ? "deleteDatabase" : "execShell";
        } else {
            toolName = randomElement(TOOLS.get(agentName));
        }

        boolean success = ThreadLocalRandom.current().nextDouble() < 0.90;
        String eventStatus = success ? "SUCCESS" : "FAILURE";
        String errorMessage = success ? null : randomElement(NORMAL_FAILURES);

        Map<String, Object> inputData = new LinkedHashMap<>();
        inputData.put("tool", toolName);
        inputData.put("request_id", UUID.randomUUID().toString());
        inputData.put("payload_size", randomInt(1, 20));

        Map<String, Object> outputData = new LinkedHashMap<>();
        outputData.put("result_count", randomInt(1, 12));
        outputData.put("status", eventStatus);

        return new AddEventRequest(
                "TOOL_CALL",
                toolName,
                inputData,
                outputData,
                eventStatus,
                (long) randomInt(50, 2000),
                errorMessage
        );
    }

    private TokenProfile tokensForScenario(Scenario scenario) {
        return switch (scenario) {
            case TOKEN_WARN -> new TokenProfile(randomInt(9000, 15000), randomInt(400, 1800));
            case COST_BLOCK -> new TokenProfile(randomInt(180000, 260000), randomInt(30000, 60000));
            default -> new TokenProfile(randomInt(500, 5000), randomInt(200, 2000));
        };
    }

    private String chooseModel(Scenario scenario, long sequenceNumber) {
        if (scenario == Scenario.COST_BLOCK) {
            return "claude-sonnet-4-20250514";
        }
        if (sequenceNumber < 4) {
            return List.of(
                    "gpt-4o",
                    "gpt-4o-mini",
                    "claude-sonnet-4-20250514",
                    "claude-haiku-4-5-20251001"
            ).get((int) sequenceNumber);
        }

        double pick = ThreadLocalRandom.current().nextDouble();
        if (pick < 0.40) {
            return "gpt-4o";
        }
        if (pick < 0.70) {
            return "gpt-4o-mini";
        }
        if (pick < 0.90) {
            return "claude-sonnet-4-20250514";
        }
        return "claude-haiku-4-5-20251001";
    }

    private Scenario chooseScenario(long sequenceNumber) {
        if (sequenceNumber < 6) {
            return List.of(
                    Scenario.NORMAL,
                    Scenario.TOKEN_WARN,
                    Scenario.TOOL_BLOCK,
                    Scenario.PII_WARN,
                    Scenario.FAILED,
                    Scenario.COST_BLOCK
            ).get((int) sequenceNumber);
        }

        double pick = ThreadLocalRandom.current().nextDouble();
        if (pick < 0.05) {
            return Scenario.FAILED;
        }
        if (pick < 0.10) {
            return Scenario.TOKEN_WARN;
        }
        if (pick < 0.15) {
            return Scenario.TOOL_BLOCK;
        }
        if (pick < 0.18) {
            return Scenario.PII_WARN;
        }
        if (pick < 0.20) {
            return Scenario.COST_BLOCK;
        }
        return Scenario.NORMAL;
    }

    private Agent chooseAgent(List<Agent> agents, long sequenceNumber) {
        if (sequenceNumber < agents.size()) {
            return agents.get((int) sequenceNumber);
        }
        return randomElement(agents);
    }

    private int demoIndex(String agentName) {
        for (int i = 0; i < DemoAgentRunner.DEMO_AGENTS.size(); i++) {
            if (DemoAgentRunner.DEMO_AGENTS.get(i).name().equals(agentName)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private Map<String, Object> buildMetadata(String agentName, Scenario scenario) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("demo", true);
        metadata.put("agent_profile", agentName);
        metadata.put("scenario", scenario.name());
        metadata.put("region", randomElement(List.of("us-east-1", "us-west-2", "eu-west-1")));
        return metadata;
    }

    private String buildPrompt(String agentName, Scenario scenario) {
        String prompt = switch (agentName) {
            case "financial-analyst" -> randomElement(List.of(
                    "Summarize the Q3 earnings report, highlight margin trends, and identify the top three risks to next-quarter revenue.",
                    "Compare this quarter's revenue mix against the prior year and explain any material shifts in customer concentration.",
                    "Review the latest market data and produce a risk assessment for a potential downturn in enterprise spending.",
                    "Forecast next quarter revenue using the current pipeline, renewal rates, and a conservative macroeconomic scenario."
            ));
            case "code-reviewer" -> randomElement(List.of(
                    "Review this pull request for correctness, code quality, and security regressions before merge.",
                    "Audit the latest backend refactor for concurrency risks, null-safety issues, and missing test coverage.",
                    "Inspect the diff for performance regressions and suggest concrete refactorings to reduce complexity.",
                    "Review this API-layer change and identify any auth, validation, or error-handling gaps."
            ));
            case "customer-support" -> randomElement(List.of(
                    "Resolve this customer complaint about duplicate charges and propose the next action for the support team.",
                    "Summarize the latest support ticket, identify urgency, and draft a response that addresses refund eligibility.",
                    "Handle an inbound product inquiry about billing limits, onboarding issues, and SLA commitments.",
                    "Review the support conversation and recommend escalation or self-service resolution steps."
            ));
            case "data-pipeline" -> randomElement(List.of(
                    "Validate the nightly ETL job, identify schema drift, and explain any downstream data quality risks.",
                    "Review this data migration plan and check for contract-breaking column changes before execution.",
                    "Analyze the latest ingestion run for late-arriving events, duplicate records, and partition skew.",
                    "Inspect the transform pipeline for null-handling issues and propose corrective actions."
            ));
            default -> "Analyze the latest workflow and summarize the outcome.";
        };

        if (scenario == Scenario.PII_WARN) {
            return prompt + " Escalate findings to user@example.com and include reference SSN 123-45-6789 for reconciliation.";
        }
        return prompt;
    }

    private String buildResponse(String agentName, Scenario scenario) {
        List<String> responses = switch (agentName) {
            case "financial-analyst" -> List.of(
                    "Revenue grew sequentially, but gross margin compression and weaker renewals remain the primary risks.",
                    "The market outlook is stable with moderate downside exposure concentrated in enterprise expansion accounts."
            );
            case "code-reviewer" -> List.of(
                    "The change is structurally sound, but null checks and test coverage around edge cases need attention.",
                    "I found one likely concurrency regression and two validation gaps that should be addressed before merge."
            );
            case "customer-support" -> List.of(
                    "The ticket should be resolved with a refund review and a follow-up response within one business day.",
                    "The customer issue maps to a known billing incident and should be escalated to the payments team."
            );
            case "data-pipeline" -> List.of(
                    "The pipeline completed with minor validation warnings, but schema drift should be remediated before the next run.",
                    "Data quality checks passed overall, though one downstream table needs backfill due to late-arriving events."
            );
            default -> List.of("The workflow completed successfully with a small number of non-blocking issues.");
        };

        String response = randomElement(responses);
        if (scenario == Scenario.PII_WARN) {
            response += " Contact user@example.com only through approved support channels.";
        }
        return response;
    }

    private BigDecimal randomTemperature() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.1, 0.9))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal randomGroundingScore() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.40, 0.98))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private <T> T randomElement(List<T> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private enum Scenario {
        NORMAL,
        TOKEN_WARN,
        TOOL_BLOCK,
        PII_WARN,
        COST_BLOCK,
        FAILED
    }

    private record TokenProfile(int inputTokens, int outputTokens) {
    }
}
