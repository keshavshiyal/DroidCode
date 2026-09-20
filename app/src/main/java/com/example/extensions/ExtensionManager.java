package com.example.extensions;

import java.util.ArrayList;
import java.util.List;

public class ExtensionManager {

    private static final ExtensionManager INSTANCE = new ExtensionManager();

    private final List<String> loadedExtensions = new ArrayList<>();

    private ExtensionManager() {}

    public static ExtensionManager getInstance() {
        return INSTANCE;
    }

    public List<String> getLoadedExtensions() {
        return loadedExtensions;
    }

    public String getStatusMessage() {
        if (loadedExtensions.isEmpty()) {
            return "No extensions installed.";
        }
        return loadedExtensions.size() + " extension(s) active.";
    }
}
