package com.applitools.eyes.appium;

public class ContentSize {

    public int height;
    public int width;
    public int top;
    public int left;
    public int scrollableOffset;
    public int touchPadding;

    public String toString() {
        return String.format("{height=%s, width=%s, top=%s, left=%s, scrollableOffset=%s, touchPadding=%s}",
            height, width, top, left, scrollableOffset, touchPadding);
    }

}
