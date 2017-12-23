package com.applitools.eyes.appium;

import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.wrappers.EyesRemoteWebElement;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebElement;

public class EyesAppiumDriver extends EyesWebDriver {

    private Map<String, Object> sessionDetails;

    public EyesAppiumDriver(Logger logger, Eyes eyes, AppiumDriver driver) {
        super(logger, eyes, driver);
    }

    @Override
    public AppiumDriver getRemoteWebDriver () { return (AppiumDriver) this.driver; }


    @Override
    public EyesAppiumElement getEyesElement (WebElement element) {
        if (element instanceof EyesAppiumElement) {
            return (EyesAppiumElement) element;
        }
        // this next if-block is just for sanity checking until everything is working
        if (element instanceof EyesRemoteWebElement) {
            throw new Error("Programming error: should not have sent an EyesRemoteWebElement in");
        }
        return new EyesAppiumElement(logger, this, element);
    }

    private Map<String, Object> getCachedSessionDetails () {
        if(sessionDetails == null) {
            logger.verbose("Retrieving session details and caching the result...");
            sessionDetails = getRemoteWebDriver().getSessionDetails();
        }
        return sessionDetails;
    }

    public HashMap<String, Integer> getViewportRect () {
        Map<String, Long> rectMap = (Map<String, Long>) getCachedSessionDetails().get("viewportRect");
        HashMap<String, Integer> intRectMap = new HashMap<String, Integer>();
        intRectMap.put("width", rectMap.get("width").intValue());
        intRectMap.put("height", rectMap.get("height").intValue());
        return intRectMap;
    }

    public int getStatusBarHeight() {
        return ((Long) getCachedSessionDetails().get("statBarHeight")).intValue();
    }

    public double getDevicePixelRatio () {
        return ((Long) getCachedSessionDetails().get("pixelRatio")).doubleValue();
    }

    /**
     * @param forceQuery If true, we will perform the query even if we have a cached viewport size.
     * @return The viewport size of the default content (outer most frame).
     */
    public RectangleSize getDefaultContentViewportSize(boolean forceQuery) {
        logger.verbose("getDefaultContentViewportSize(forceQuery: " + forceQuery + ")");

        if (defaultContentViewportSize != null && !forceQuery) {
            logger.verbose("Using cached viewport size: " + defaultContentViewportSize);
            return defaultContentViewportSize;
        }

        HashMap<String, Integer> rect = getViewportRect();
        defaultContentViewportSize = new RectangleSize(rect.get("width"), rect.get("height"));
        logger.verbose("Done! Viewport size: " + defaultContentViewportSize);

        return defaultContentViewportSize;
    }
}
