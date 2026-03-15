package dev.ayush.agentlens.demo;

import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.agent.AgentService;
import dev.ayush.agentlens.agent.dto.CreateAgentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentlens.demo.enabled", havingValue = "true")
@Slf4j
public class DemoAgentRunner {

    static final List<DemoAgentDefinition> DEMO_AGENTS = List.of(
            new DemoAgentDefinition("financial-analyst", "Analyzes financial reports and market data"),
            new DemoAgentDefinition("code-reviewer", "Reviews pull requests and suggests improvements"),
            new DemoAgentDefinition("customer-support", "Handles customer queries and ticket resolution"),
            new DemoAgentDefinition("data-pipeline", "Processes and transforms data across systems")
    );

    private final AgentRepository agentRepository;
    private final AgentService agentService;

    @EventListener(ApplicationReadyEvent.class)
    public void registerDemoAgents() {
        for (DemoAgentDefinition definition : DEMO_AGENTS) {
            if (agentRepository.findByName(definition.name()).isEmpty()) {
                agentService.createAgent(new CreateAgentRequest(
                        definition.name(),
                        definition.description(),
                        "demo-system"
                ));
                log.info("Registered demo agent {}", definition.name());
            }
        }
    }

    record DemoAgentDefinition(String name, String description) {
    }
}
