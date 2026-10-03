package com.droidcode.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Memory-efficient, differential Undo/Redo manager for text buffers.
 * Instead of storing full-text snapshots on every keystroke (which causes severe
 * GC churn and OOM on large files), this engine stores piecewise character deltas.
 */
public class UndoManager {

    private static final int MAX_HISTORY = 100;
    private static final int SNAPSHOT_THRESHOLD_BYTES = 2048;

    public static class TextDelta {
        public final int offset;
        public final String deletedText;
        public final String insertedText;
        public final String fallbackPriorState;

        public TextDelta(int offset, String deletedText, String insertedText, String fallbackPriorState) {
            this.offset = offset;
            this.deletedText = deletedText != null ? deletedText : "";
            this.insertedText = insertedText != null ? insertedText : "";
            this.fallbackPriorState = fallbackPriorState;
        }
    }

    private final Deque<TextDelta> undoStack = new ArrayDeque<>();
    private final Deque<TextDelta> redoStack = new ArrayDeque<>();

    private String lastState = null;

    public synchronized void pushState(String state) {
        if (state == null) return;
        if (lastState != null && lastState.equals(state)) {
            return;
        }

        if (lastState == null) {
            lastState = state;
            return;
        }

        TextDelta delta = computeDelta(lastState, state);
        undoStack.push(delta);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
        lastState = state;
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized String undo(String currentState) {
        if (!canUndo()) return currentState != null ? currentState : (lastState != null ? lastState : "");

        TextDelta delta = undoStack.pop();
        redoStack.push(delta);

        String base = currentState != null ? currentState : lastState;
        if (base == null) base = "";

        // Check if delta applies cleanly
        if (delta.offset >= 0 &&
                delta.offset + delta.insertedText.length() <= base.length() &&
                base.startsWith(delta.insertedText, delta.offset)) {
            String undone = base.substring(0, delta.offset) +
                    delta.deletedText +
                    base.substring(delta.offset + delta.insertedText.length());
            lastState = undone;
            return undone;
        }

        // Fallback to snapshot if available
        if (delta.fallbackPriorState != null) {
            lastState = delta.fallbackPriorState;
            return delta.fallbackPriorState;
        }

        lastState = base;
        return base;
    }

    public synchronized String redo(String currentState) {
        if (!canRedo()) return currentState != null ? currentState : (lastState != null ? lastState : "");

        TextDelta delta = redoStack.pop();
        undoStack.push(delta);

        String base = currentState != null ? currentState : lastState;
        if (base == null) base = "";

        // Check if delta applies cleanly
        if (delta.offset >= 0 &&
                delta.offset + delta.deletedText.length() <= base.length() &&
                base.startsWith(delta.deletedText, delta.offset)) {
            String redone = base.substring(0, delta.offset) +
                    delta.insertedText +
                    base.substring(delta.offset + delta.deletedText.length());
            lastState = redone;
            return redone;
        }

        lastState = base;
        return base;
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
        lastState = null;
    }

    public static TextDelta computeDelta(String oldText, String newText) {
        if (oldText == null) oldText = "";
        if (newText == null) newText = "";

        int prefix = 0;
        int minLen = Math.min(oldText.length(), newText.length());
        while (prefix < minLen && oldText.charAt(prefix) == newText.charAt(prefix)) {
            prefix++;
        }

        int oldSuffix = oldText.length() - 1;
        int newSuffix = newText.length() - 1;
        while (oldSuffix >= prefix && newSuffix >= prefix && oldText.charAt(oldSuffix) == newText.charAt(newSuffix)) {
            oldSuffix--;
            newSuffix--;
        }

        String deleted = oldText.substring(prefix, oldSuffix + 1);
        String inserted = newText.substring(prefix, newSuffix + 1);
        String fallback = oldText.length() <= SNAPSHOT_THRESHOLD_BYTES ? oldText : null;

        return new TextDelta(prefix, deleted, inserted, fallback);
    }
}
