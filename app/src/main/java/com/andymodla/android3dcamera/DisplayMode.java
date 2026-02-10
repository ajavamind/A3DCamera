package com.andymodla.android3dcamera;

public enum DisplayMode {
    SBS, ANAGLYPH, LEFT, RIGHT;

    public DisplayMode next() {
        // Get all constants in the order they are declared
        DisplayMode[] values = DisplayMode.values();
        // Calculate next index and wrap back to 0 using modulo
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    // Cycles backward: LEFT -> ANAGLYPH -> SBS -> RIGHT
    public DisplayMode previous() {
        DisplayMode[] values = values();
        // Adding values.length handles the negative result of (0 - 1)
        int prevIndex = (this.ordinal() - 1 + values.length) % values.length;
        return values[prevIndex];
    }

}