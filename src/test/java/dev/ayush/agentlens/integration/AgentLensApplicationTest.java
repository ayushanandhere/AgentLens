package dev.ayush.agentlens.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.audit.AuditLogRepository;
import dev.ayush.agentlens.policy.Policy;
import dev.ayush.agentlens.policy.PolicyRepository;
import dev.ayush.agentlens.policy.PolicyViolationRepository;
import dev.ayush.agentlens.trace.TraceEventRepository;
import dev.ayush.agentlens.trace.TraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class AgentLensApplicationTest {

    private static final String INGEST_KEY = "ingest-test-key";
    private static final String OPERATOR_KEY = "operator-test-key";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0").asCompatibleSubstituteFor("apache/kafka"));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("agentlens.demo.enabled", () -> "false");
        registry.add("otel.sdk.disabled", () -> "true");
        registry.add("agentlens.security.api-keys[0].name", () -> "test-ingest");
        registry.add("agentlens.security.api-keys[0].key", () -> INGEST_KEY);
        registry.add("agentlens.security.api-keys[0].scope", () -> "INGEST");
        registry.add("agentlens.security.api-keys[1].name", () -> "test-operator");
        registry.add("agentlens.security.api-keys[1].key", () -> OPERATOR_KEY);
        registry.add("agentlens.security.api-keys[1].scope", () -> "OPERATOR");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private TraceRepository traceRepository;
    @Autowired
    private TraceEventRepository traceEventRepository;
    @Autowired
    private PolicyViolationRepository policyViolationRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private Agent agent;

    @BeforeEach
    void setUp() {
        policyViolationRepository.deleteAll();
        traceEventRepository.deleteAll();
        traceRepository.deleteAll();
        policyRepository.deleteAll();
        agentRepository.deleteAll();
        auditLogRepository.deleteAll();

        agent = agentRepository.save(Agent.builder()
                .name("integration-agent")
                .owner("tests")
                .status("ACTIVE")
                .build());

        policyRepository.save(Policy.builder()
                .name("Approval Policy")
                .policyType("REQUIRE_APPROVAL")
                .config(Map.of("for_tools", java.util.List.of("execShell"), "approvers", java.util.List.of("approver@agentlens.dev")))
                .scope("GLOBAL")
                .severity("BLOCK")
                .enabled(true)
                .build());
    }

    @Test
    void approvalWorkflowAndDryRunEndpointsWorkEndToEnd() throws Exception {
        String traceResponse = mockMvc.perform(post("/api/v1/traces")
                        .header("X-API-Key", INGEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "%s",
                                  "model": "gpt-4o",
                                  "promptText": "Review the deployment plan",
                                  "tenantId": "tenant-acme",
                                  "sessionId": "session-it"
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String traceId = objectMapper.readTree(traceResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/traces/{id}/events", traceId)
                        .header("X-API-Key", INGEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "TOOL_CALL",
                                  "eventName": "execShell",
                                  "inputData": {"command": "kubectl rollout restart"},
                                  "outputData": {"status": "pending"},
                                  "status": "SUCCESS",
                                  "durationMs": 75
                                }
                                """))
                .andExpect(status().isConflict());

        String violationsJson = mockMvc.perform(get("/api/v1/violations")
                        .header("X-API-Key", OPERATOR_KEY)
                        .param("actionTaken", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode violations = objectMapper.readTree(violationsJson).get("content");
        assertThat(violations.size()).isEqualTo(1);
        String violationId = violations.get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/violations/{id}/approve", violationId)
                        .header("X-API-Key", OPERATOR_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resolvedBy":"approver@agentlens.dev"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/traces/{id}/complete", traceId)
                        .header("X-API-Key", INGEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "responseText": "Approved and completed",
                                  "inputTokens": 1200,
                                  "outputTokens": 400,
                                  "groundingScore": 0.82,
                                  "ttftMs": 110
                                }
                                """))
                .andExpect(status().isOk());

        String timelineJson = mockMvc.perform(get("/api/v1/traces/{id}/timeline", traceId)
                        .header("X-API-Key", OPERATOR_KEY))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(timelineJson).get("items").size()).isEqualTo(1);

        String dryRunJson = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .header("X-API-Key", OPERATOR_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "%s",
                                  "model": "gpt-4o",
                                  "promptText": "Run this shell command",
                                  "events": [
                                    {
                                      "eventType": "TOOL_CALL",
                                      "eventName": "execShell",
                                      "inputData": {"command": "deploy.sh"},
                                      "outputData": {"status":"pending"}
                                    }
                                  ]
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(dryRunJson).get("overallVerdict").asText()).isEqualTo("PENDING_APPROVAL");

        waitForAuditLogs();
        assertThat(auditLogRepository.count()).isGreaterThan(0);
    }

    private void waitForAuditLogs() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (auditLogRepository.count() > 0) {
                return;
            }
            Thread.sleep(250);
        }
    }
}
