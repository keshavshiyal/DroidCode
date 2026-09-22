package com.droidcode.database;

public interface DatabaseProvider {
    String getProviderId();
    String getDisplayName();
    boolean isConnected();
    String executeQuery(String sql) throws Exception;
}
