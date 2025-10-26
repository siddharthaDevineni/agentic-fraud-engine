package com.agenticfraud.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupBanner {

    private static final Logger logger = LoggerFactory.getLogger(StartupBanner.class);
    private final Environment environment;

    public StartupBanner(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isCodespaces = activeProfiles.length > 0 && activeProfiles[0].equals("codespaces");

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("AGENTIC FRAUD ENGINE - READY");
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (isCodespaces) {
            logger.info("AI Mode: LOCAL LLM (Ollama - Llama 3.1 8B)");
            logger.info("Expected response time: ~20-30 seconds");
            logger.info("No API key needed - perfect for demos!");
        } else {
            logger.info("AI Mode: GROQ API (Cloud - Fast)");
            logger.info("Expected response time: ~5-10 seconds");
            logger.info("Requires GROQ_API_KEY environment variable");
        }

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("Kafka UI: http://localhost:8090");
        logger.info("API: http://localhost:8080");
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
