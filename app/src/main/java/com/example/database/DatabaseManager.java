package com.example.database;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final List<DatabaseProvider> providers = new ArrayList<>();
    private DatabaseProvider activeProvider;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public List<DatabaseProvider> getProviders() {
        return providers;
    }

    public DatabaseProvider getActiveProvider() {
        return activeProvider;
    }

    public boolean hasActiveConnection() {
        return activeProvider != null && activeProvider.isConnected();
    }

    public String getStatusMessage() {
        if (!hasActiveConnection()) {
            return "No active database connection.";
        }
        return "Connected to " + activeProvider.getDisplayName();
    }
}
