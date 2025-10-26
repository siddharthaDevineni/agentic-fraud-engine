package com.agenticfraud.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class ChatModelConfig {

  private static final Logger logger = LoggerFactory.getLogger(ChatModelConfig.class);

  /** Codespaces profile: Make Ollama (local LLM) the primary ChatModel */
  @Bean
  @Primary
  @Profile("codespaces")
  public ChatModel primaryChatModelForCodespaces(
      @Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {

    logger.info("Using Ollama ChatModel as primary (Local LLM)");
    logger.info("Model class: {}", ollamaChatModel.getClass().getSimpleName());

    return ollamaChatModel;
  }

  /** Default profile: Use OpenAI/Groq API as the primary ChatModel */
  @Bean
  @Primary
  @Profile("default")
  public ChatModel primaryChatModelForDefault(
      @Qualifier("openAiChatModel") ChatModel openAiChatModel) {

    logger.info("Using OpenAI ChatModel as primary (Groq API)");
    logger.info("Model class: {}", openAiChatModel.getClass().getSimpleName());

    return openAiChatModel;
  }
}
