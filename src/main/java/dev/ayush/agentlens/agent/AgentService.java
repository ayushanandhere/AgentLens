package dev.ayush.agentlens.agent;

import dev.ayush.agentlens.agent.dto.AgentResponse;
import dev.ayush.agentlens.agent.dto.CreateAgentRequest;
import dev.ayush.agentlens.agent.dto.UpdateAgentRequest;
import dev.ayush.agentlens.common.exception.ResourceNotFoundException;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.trace.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final TraceRepository traceRepository;
    private final AuditEventProducer auditEventProducer;

    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request) {
        Agent agent = Agent.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(request.getOwner())
                .build();
        agent = agentRepository.save(agent);
        auditEventProducer.publishAgentCreated(agent);
        return toResponse(agent, 0L);
    }

    @Transactional(readOnly = true)
    public AgentResponse getAgent(UUID id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
        long traceCount = traceRepository.countByAgentId(id);
        return toResponse(agent, traceCount);
    }

    @Transactional(readOnly = true)
    public Page<AgentResponse> listAgents(Pageable pageable) {
        return agentRepository.findAll(pageable).map(agent -> {
            long traceCount = traceRepository.countByAgentId(agent.getId());
            return toResponse(agent, traceCount);
        });
    }

    @Transactional
    public AgentResponse updateAgent(UUID id, UpdateAgentRequest request) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));

        if (request.getName() != null) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            agent.setStatus(request.getStatus());
        }
        agent.setUpdatedAt(LocalDateTime.now());
        agent = agentRepository.save(agent);
        auditEventProducer.publishAgentUpdated(agent);

        long traceCount = traceRepository.countByAgentId(id);
        return toResponse(agent, traceCount);
    }

    @Transactional
    public AgentResponse killAgent(UUID id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
        agent.setStatus("KILLED");
        agent.setUpdatedAt(LocalDateTime.now());
        agent = agentRepository.save(agent);
        auditEventProducer.publishAgentKilled(agent);

        long traceCount = traceRepository.countByAgentId(id);
        return toResponse(agent, traceCount);
    }

    private AgentResponse toResponse(Agent agent, Long traceCount) {
        return AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .owner(agent.getOwner())
                .status(agent.getStatus())
                .traceCount(traceCount)
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}
