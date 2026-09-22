package com.droidcode.ai;

public class AiService {

    private static final AiService INSTANCE = new AiService();

    private AiProvider activeProvider;

    private AiService() {}

    public static AiService getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        return activeProvider != null && activeProvider.isConfigured();
    }

    public void setProvider(AiProvider provider) {
        this.activeProvider = provider;
    }

    public String getStatusMessage() {
        if (!isConfigured()) {
            return "AI service not configured. API key is required in Settings.";
        }
        return "AI service active: " + activeProvider.getProviderName();
    }
}
