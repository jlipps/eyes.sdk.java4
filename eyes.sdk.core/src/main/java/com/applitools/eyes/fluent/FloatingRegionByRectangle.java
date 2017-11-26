package com.applitools.eyes.fluent;

import com.applitools.eyes.*;

public class FloatingRegionByRectangle implements GetFloatingRegion {
    private Region rect;
    private int maxUpOffset;
    private int maxDownOffset;
    private int maxLeftOffset;
    private int maxRightOffset;

    public FloatingRegionByRectangle(Region rect, int maxUpOffset, int maxDownOffset, int maxLeftOffset, int maxRightOffset) {
        this.rect = rect;
        this.maxUpOffset = maxUpOffset;
        this.maxDownOffset = maxDownOffset;
        this.maxLeftOffset = maxLeftOffset;
        this.maxRightOffset = maxRightOffset;
    }

    @Override
    public FloatingMatchSettings getRegion(EyesBase eyesBase, EyesScreenshot screenshot) {
        return new FloatingMatchSettings(
                rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight(),
                maxUpOffset, maxDownOffset, maxLeftOffset, maxRightOffset);
    }
}
