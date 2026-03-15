package dev.ayush.agentlens.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_AGENT_TRACES = "agent-traces";
    public static final String TOPIC_POLICY_VIOLATIONS = "policy-violations";
    public static final String TOPIC_AUDIT_EVENTS = "audit-events";

    @Bean
    public NewTopic agentTracesTopic() {
        return new NewTopic(TOPIC_AGENT_TRACES, 1, (short) 1);
    }

    @Bean
    public NewTopic policyViolationsTopic() {
        return new NewTopic(TOPIC_POLICY_VIOLATIONS, 1, (short) 1);
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return new NewTopic(TOPIC_AUDIT_EVENTS, 1, (short) 1);
    }
}
