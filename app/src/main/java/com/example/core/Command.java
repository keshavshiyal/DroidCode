package com.example.core;

import androidx.annotation.NonNull;
import java.util.function.Supplier;

public class Command {

    private final String id;
    private final String title;
    private final String category;
    private final String shortcut;
    private final boolean enabled;
    private final Supplier<Boolean> enabledSupplier;
    private final Runnable action;

    public Command(@NonNull String id, @NonNull String title, @NonNull String category, String shortcut, boolean enabled, Runnable action) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.shortcut = shortcut;
        this.enabled = enabled;
        this.enabledSupplier = null;
        this.action = action;
    }

    public Command(@NonNull String id, @NonNull String title, @NonNull String category, String shortcut, Supplier<Boolean> enabledSupplier, Runnable action) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.shortcut = shortcut;
        this.enabled = true;
        this.enabledSupplier = enabledSupplier;
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
        if (enabledSupplier != null) {
            try {
                Boolean val = enabledSupplier.get();
                return val != null && val;
            } catch (Exception e) {
                return false;
            }
        }
        return enabled;
    }

    public void execute() {
        if (isEnabled() && action != null) {
            action.run();
        }
    }
}
