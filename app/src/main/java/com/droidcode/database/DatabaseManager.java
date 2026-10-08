package com.droidcode.database;

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

    public synchronized void registerProvider(DatabaseProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            if (activeProvider == null) {
                activeProvider = provider;
            }
        }
    }

    public synchronized void setActiveProvider(DatabaseProvider provider) {
        this.activeProvider = provider;
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
        }
    }

    public synchronized void unregisterProvider(DatabaseProvider provider) {
        providers.remove(provider);
        if (activeProvider == provider) {
            activeProvider = providers.isEmpty() ? null : providers.get(0);
        }
    }

    public String getStatusMessage() {
        if (!hasActiveConnection()) {
            return "No active database connection.";
        }
        return "Connected to " + activeProvider.getDisplayName();
    }
}
