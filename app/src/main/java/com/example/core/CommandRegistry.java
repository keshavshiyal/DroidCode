package com.example.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegistry {

    private static final CommandRegistry INSTANCE = new CommandRegistry();

    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    private CommandRegistry() {}

    public static CommandRegistry getInstance() {
        return INSTANCE;
    }

    public void registerCommand(Command command) {
        commands.put(command.getId(), command);
    }

    public void unregisterCommand(String id) {
        commands.remove(id);
    }

    public Command getCommand(String id) {
        return commands.get(id);
    }

    public List<Command> getAllCommands() {
        List<Command> list = new ArrayList<>(commands.values());
        Collections.sort(list, (c1, c2) -> {
            int catComp = c1.getCategory().compareToIgnoreCase(c2.getCategory());
            if (catComp != 0) return catComp;
            return c1.getTitle().compareToIgnoreCase(c2.getTitle());
        });
        return list;
    }

    public List<Command> searchCommands(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllCommands();
        }
        String lower = query.toLowerCase().trim();
        List<Command> results = new ArrayList<>();
        for (Command cmd : commands.values()) {
            if (cmd.getTitle().toLowerCase().contains(lower) ||
                cmd.getCategory().toLowerCase().contains(lower) ||
                cmd.getId().toLowerCase().contains(lower) ||
                (cmd.getShortcut() != null && cmd.getShortcut().toLowerCase().contains(lower))) {
                results.add(cmd);
            }
        }
        Collections.sort(results, (c1, c2) -> c1.getTitle().compareToIgnoreCase(c2.getTitle()));
        return results;
    }

    public List<Command> getCommandsByCategory(String category) {
        List<Command> list = new ArrayList<>();
        for (Command cmd : commands.values()) {
            if (cmd.getCategory().equalsIgnoreCase(category)) {
                list.add(cmd);
            }
        }
        return list;
    }

    public boolean executeCommand(String id) {
        Command cmd = commands.get(id);
        if (cmd != null && cmd.isEnabled()) {
            cmd.execute();
            return true;
        }
        return false;
    }

    public void clear() {
        commands.clear();
    }
}
