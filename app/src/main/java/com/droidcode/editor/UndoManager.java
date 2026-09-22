package com.droidcode.editor;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {

    private static final int MAX_HISTORY = 100;

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();

    public void pushState(String state) {
        if (state == null) return;
        if (!undoStack.isEmpty() && undoStack.peek().equals(state)) {
            return;
        }
        undoStack.push(state);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public boolean canUndo() {
        return undoStack.size() > 1;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public String undo(String currentState) {
        if (!canUndo()) return currentState;
        String top = undoStack.pop();
        redoStack.push(top);
        return undoStack.peek();
    }

    public String redo(String currentState) {
        if (!canRedo()) return currentState;
        String restored = redoStack.pop();
        undoStack.push(restored);
        return restored;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
