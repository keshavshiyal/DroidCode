package com.example.database;

public interface DatabaseProvider {
    String getProviderId();
    String getDisplayName();
    boolean isConnected();
    String executeQuery(String sql) throws Exception;
}
