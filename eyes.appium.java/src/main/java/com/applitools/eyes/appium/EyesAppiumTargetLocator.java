package com.applitools.eyes.appium;

import com.applitools.eyes.Logger;
import com.applitools.eyes.selenium.wrappers.EyesTargetLocator;
import com.applitools.utils.ArgumentGuard;
import io.appium.java_client.MobileBy;
import java.util.List;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EyesAppiumTargetLocator extends EyesTargetLocator {

    private final EyesAppiumDriver driver; // intentionally hiding parent driver field
    private final AppiumScrollPositionProvider scrollPosition; // intentionally hiding parent scrollPosition field

    /**
     * Initialized a new EyesWebTargetLocator object.
     *
     * @param driver The AppiumDriver for which we are going to mock out target locator
     * @param targetLocator The actual TargetLocator object. We are only ever going to use this for non-frames, however, since for us a "frame" is really a scrollview and doesn't need to be switched to
     */
    public EyesAppiumTargetLocator(Logger logger, EyesAppiumDriver driver,
        WebDriver.TargetLocator targetLocator) {
        super(logger, targetLocator);
        ArgumentGuard.notNull(driver, "driver");
        this.driver = driver;
        this.scrollPosition = new AppiumScrollPositionProvider(logger, driver);
    }

    protected EyesAppiumDriver getDriver () { return driver; }
    protected AppiumScrollPositionProvider getScrollProvider () { return scrollPosition; }

    protected void switchToDefault () {
        logger.verbose("No need to switch to default since we are dealing with Appium scrollviews");
    }

    protected void switchToParent () {
        logger.verbose("No need to switch to parent since we are dealing with Appium scrollviews");
    }

    protected void switchToFrame (WebElement frame) {
        logger.verbose("No need to switch to frame since we are dealing with Appium scrollviews");
    }

    protected void switchToFrame (String nameOrId) {
        logger.verbose("No need to switch to frame since we are dealing with Appium scrollviews");
    }

    protected void switchToWindow (String nameOrId) {
        // TODO verify we should do nothing instead of actually call the window commands
        logger.verbose("Not switching to window since we are in Appium and windows don't make sense as part of scroll view chains");
    }

    protected WebElement getFrameByIndex(int index) {
        logger.verbose("Getting views list...");
        List<WebElement> views = driver.findElementsByXPath("//*[@scrollable='true']");
        if (index > views.size()) {
            throw new NoSuchFrameException(String.format("Scrollview index [%d] is invalid!", index));
        }
        logger.verbose("Done! getting the specific view...");
        return views.get(index);
    }

    protected List<WebElement> getFramesByName (String name) {
        return driver.findElements(MobileBy.AccessibilityId(name));
    }

}
