package dev.ayush.agentlens.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "agentlens.security")
@Getter
@Setter
public class SecurityProperties {

    private List<ApiKeyConfig> apiKeys = new ArrayList<>();
    private List<String> allowedOrigins = new ArrayList<>();

    public Optional<ApiKeyConfig> findByKey(String key) {
        return apiKeys.stream().filter(config -> config.getKey().equals(key)).findFirst();
    }

    @Getter
    @Setter
    public static class ApiKeyConfig {
        private String name;
        private String key;
        private ApiKeyScope scope;
    }
}
