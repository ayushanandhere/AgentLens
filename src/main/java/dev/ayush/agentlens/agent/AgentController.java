package dev.ayush.agentlens.agent;

import dev.ayush.agentlens.agent.dto.AgentResponse;
import dev.ayush.agentlens.agent.dto.CreateAgentRequest;
import dev.ayush.agentlens.agent.dto.UpdateAgentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentResponse createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return agentService.createAgent(request);
    }

    @GetMapping
    public Page<AgentResponse> listAgents(Pageable pageable) {
        return agentService.listAgents(pageable);
    }

    @GetMapping("/{id}")
    public AgentResponse getAgent(@PathVariable UUID id) {
        return agentService.getAgent(id);
    }

    @PatchMapping("/{id}")
    public AgentResponse updateAgent(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateAgentRequest request) {
        return agentService.updateAgent(id, request);
    }

    @PostMapping("/{id}/kill")
    public AgentResponse killAgent(@PathVariable UUID id) {
        return agentService.killAgent(id);
    }
}
