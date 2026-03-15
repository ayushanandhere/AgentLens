package dev.ayush.agentlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentLensApplication.class, args);
    }
}
