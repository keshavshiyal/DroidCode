package com.droidcode.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Memory-efficient, differential Undo/Redo manager for text buffers.
 * Instead of storing full-text snapshots on every keystroke (which causes severe
 * GC churn and OOM on large files), this engine stores piecewise character deltas
 * along with cursor position history.
 */
public class UndoManager {

    private static final int MAX_HISTORY = 100;
    private static final int SNAPSHOT_THRESHOLD_BYTES = 2048;

    public static class TextDelta {
        public final int offset;
        public final String deletedText;
        public final String insertedText;
        public final String fallbackPriorState;
        public final int priorCursorOffset;
        public final int newCursorOffset;

        public TextDelta(int offset, String deletedText, String insertedText, String fallbackPriorState) {
            this(offset, deletedText, insertedText, fallbackPriorState, -1, -1);
        }

        public TextDelta(int offset, String deletedText, String insertedText, String fallbackPriorState,
                         int priorCursorOffset, int newCursorOffset) {
            this.offset = offset;
            this.deletedText = deletedText != null ? deletedText : "";
            this.insertedText = insertedText != null ? insertedText : "";
            this.fallbackPriorState = fallbackPriorState;
            this.priorCursorOffset = priorCursorOffset;
            this.newCursorOffset = newCursorOffset;
        }
    }

    public static class UndoResult {
        public final String text;
        public final int cursorOffset;

        public UndoResult(String text, int cursorOffset) {
            this.text = text != null ? text : "";
            this.cursorOffset = cursorOffset;
        }
    }

    private final Deque<TextDelta> undoStack = new ArrayDeque<>();
    private final Deque<TextDelta> redoStack = new ArrayDeque<>();

    private String lastState = null;
    private int lastCursorOffset = -1;

    public synchronized void pushState(String state) {
        pushState(state, -1, -1);
    }

    public synchronized void pushState(String state, int cursorOffset) {
        pushState(state, lastCursorOffset, cursorOffset);
    }

    public synchronized void pushState(String state, int priorCursorOffset, int newCursorOffset) {
        if (state == null) return;
        if (lastState != null && lastState.equals(state)) {
            lastCursorOffset = newCursorOffset >= 0 ? newCursorOffset : lastCursorOffset;
            return;
        }

        if (lastState == null) {
            lastState = state;
            lastCursorOffset = newCursorOffset >= 0 ? newCursorOffset : priorCursorOffset;
            return;
        }

        int priorCursor = priorCursorOffset >= 0 ? priorCursorOffset : lastCursorOffset;
        TextDelta delta = computeDelta(lastState, state, priorCursor, newCursorOffset);
        undoStack.push(delta);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
        lastState = state;
        lastCursorOffset = newCursorOffset;
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized String undo(String currentState) {
        return undoWithCursor(currentState).text;
    }

    public synchronized UndoResult undoWithCursor(String currentState) {
        if (!canUndo()) {
            String fallback = currentState != null ? currentState : (lastState != null ? lastState : "");
            return new UndoResult(fallback, lastCursorOffset);
        }

        TextDelta delta = undoStack.pop();
        redoStack.push(delta);

        String base = currentState != null ? currentState : lastState;
        if (base == null) base = "";

        int restoredCursor = delta.priorCursorOffset;
        if (restoredCursor < 0 && delta.offset >= 0) {
            restoredCursor = delta.offset + delta.deletedText.length();
        }

        // Check if delta applies cleanly
        if (delta.offset >= 0 &&
                delta.offset + delta.insertedText.length() <= base.length() &&
                base.startsWith(delta.insertedText, delta.offset)) {
            String undone = base.substring(0, delta.offset) +
                    delta.deletedText +
                    base.substring(delta.offset + delta.insertedText.length());
            lastState = undone;
            lastCursorOffset = restoredCursor >= 0 ? Math.min(restoredCursor, undone.length()) : -1;
            return new UndoResult(undone, lastCursorOffset);
        }

        // Fallback to snapshot if available
        if (delta.fallbackPriorState != null) {
            lastState = delta.fallbackPriorState;
            lastCursorOffset = restoredCursor >= 0 ? Math.min(restoredCursor, lastState.length()) : -1;
            return new UndoResult(delta.fallbackPriorState, lastCursorOffset);
        }

        lastState = base;
        lastCursorOffset = restoredCursor >= 0 ? Math.min(restoredCursor, base.length()) : -1;
        return new UndoResult(base, lastCursorOffset);
    }

    public synchronized String redo(String currentState) {
        return redoWithCursor(currentState).text;
    }

    public synchronized UndoResult redoWithCursor(String currentState) {
        if (!canRedo()) {
            String fallback = currentState != null ? currentState : (lastState != null ? lastState : "");
            return new UndoResult(fallback, lastCursorOffset);
        }

        TextDelta delta = redoStack.pop();
        undoStack.push(delta);

        String base = currentState != null ? currentState : lastState;
        if (base == null) base = "";

        int restoredCursor = delta.newCursorOffset;
        if (restoredCursor < 0 && delta.offset >= 0) {
            restoredCursor = delta.offset + delta.insertedText.length();
        }

        // Check if delta applies cleanly
        if (delta.offset >= 0 &&
                delta.offset + delta.deletedText.length() <= base.length() &&
                base.startsWith(delta.deletedText, delta.offset)) {
            String redone = base.substring(0, delta.offset) +
                    delta.insertedText +
                    base.substring(delta.offset + delta.deletedText.length());
            lastState = redone;
            lastCursorOffset = restoredCursor >= 0 ? Math.min(restoredCursor, redone.length()) : -1;
            return new UndoResult(redone, lastCursorOffset);
        }

        lastState = base;
        lastCursorOffset = restoredCursor >= 0 ? Math.min(restoredCursor, base.length()) : -1;
        return new UndoResult(base, lastCursorOffset);
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
        lastState = null;
        lastCursorOffset = -1;
    }

    public static TextDelta computeDelta(String oldText, String newText) {
        return computeDelta(oldText, newText, -1, -1);
    }

    public static TextDelta computeDelta(String oldText, String newText, int priorCursor, int newCursor) {
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

        return new TextDelta(prefix, deleted, inserted, fallback, priorCursor, newCursor);
    }
}
