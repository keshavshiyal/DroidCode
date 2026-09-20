package com.example.terminal;

public class TerminalSession {

    private final String id;
    private final String title;
    private final StringBuilder outputBuffer;
    private boolean isActive;

    public TerminalSession(String id, String title) {
        this.id = id;
        this.title = title;
        this.outputBuffer = new StringBuilder("Terminal initialized.\n");
        this.isActive = true;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOutput() {
        return outputBuffer.toString();
    }

    public void appendOutput(String text) {
        if (text != null) {
            outputBuffer.append(text);
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void terminate() {
        this.isActive = false;
        appendOutput("\nProcess exited.\n");
    }
}
