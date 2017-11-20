package com.applitools.eyes.selenium.fluent;

import com.applitools.eyes.*;
import com.applitools.eyes.fluent.GetRegion;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class IgnoreRegionByElement implements GetRegion {
    WebElement element;

    public IgnoreRegionByElement(WebElement element){
        this.element = element;
    }

    @Override
    public Region getRegion(EyesBase eyesBase, EyesScreenshot screenshot) {
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
