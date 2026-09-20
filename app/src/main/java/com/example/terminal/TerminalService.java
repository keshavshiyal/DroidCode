package com.example.terminal;

import java.util.ArrayList;
import java.util.List;

public class TerminalService {

    private static final TerminalService INSTANCE = new TerminalService();

    private final List<TerminalSession> sessions = new ArrayList<>();
    private boolean isRuntimeConfigured = false;

    private TerminalService() {}

    public static TerminalService getInstance() {
        return INSTANCE;
    }

    public boolean isRuntimeConfigured() {
        return isRuntimeConfigured;
    }

    public void setRuntimeConfigured(boolean configured) {
        this.isRuntimeConfigured = configured;
    }

    public List<TerminalSession> getSessions() {
        return sessions;
    }

    public TerminalSession createSession(String title) {
        TerminalSession session = new TerminalSession(
                "term_" + System.currentTimeMillis(),
                title != null ? title : "Shell " + (sessions.size() + 1)
        );
        sessions.add(session);
        return session;
    }

    public String getStatusMessage() {
        if (!isRuntimeConfigured) {
            return "Terminal runtime not configured. Local shell execution requires Android terminal provider plugin in Stage 3.";
        }
        return "Terminal engine ready.";
    }
}
