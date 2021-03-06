package com.applitools.eyes.selenium.fluent;

import com.applitools.eyes.*;
import com.applitools.eyes.fluent.GetRegion;
import com.applitools.eyes.selenium.Eyes;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class IgnoreRegionBySelector implements GetRegion {
    private By selector;

    public IgnoreRegionBySelector(By selector) {
        this.selector = selector;
    }

    @Override
    public Region getRegion(EyesBase eyesBase, EyesScreenshot screenshot) {
        WebElement element = ((Eyes)eyesBase).getDriver().findElement(this.selector);
        Point p = element.getLocation();
        Location l = new Location(p.getX(), p.getY());
        Location lTag = screenshot.convertLocation(l, CoordinatesType.CONTEXT_RELATIVE, CoordinatesType.SCREENSHOT_AS_IS);
        return new Region(
                lTag.getX(),
                lTag.getY(),
                element.getSize().getWidth(),
                element.getSize().getHeight());
    }
}
