package com.droidcode.ai;

public interface AiProvider {
    String getProviderName();
    boolean isConfigured();
    String generateCompletion(String prompt) throws Exception;
}
