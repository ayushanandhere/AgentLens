package dev.ayush.agentlens;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiMetadataController {

    @GetMapping("/")
    public Map<String, String> metadata() {
        return Map.of(
                "name", "AgentLens",
                "version", "0.0.1-SNAPSHOT",
                "dashboardUrl", "http://localhost:5173",
                "healthUrl", "/actuator/health",
                "auth", "All /api/v1/* endpoints require X-API-Key.",
                "docs", "See README.md and docs/api-examples.http for local usage."
        );
    }
}
