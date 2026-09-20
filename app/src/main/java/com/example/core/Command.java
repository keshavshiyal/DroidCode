package com.example.core;

import androidx.annotation.NonNull;

public class Command {

    private final String id;
    private final String title;
    private final String category;
    private final String shortcut;
    private final boolean enabled;
    private final Runnable action;

    public Command(@NonNull String id, @NonNull String title, @NonNull String category, String shortcut, boolean enabled, Runnable action) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.shortcut = shortcut;
        this.enabled = enabled;
        this.action = action;
    }

    public Command(@NonNull String id, @NonNull String title, @NonNull String category, String shortcut, Runnable action) {
        this(id, title, category, shortcut, true, action);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public String getShortcut() {
        return shortcut;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void execute() {
        if (enabled && action != null) {
            action.run();
        }
    }
}
