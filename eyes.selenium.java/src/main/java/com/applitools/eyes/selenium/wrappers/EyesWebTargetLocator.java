/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes.selenium.wrappers;

import com.applitools.eyes.Logger;
import com.applitools.eyes.selenium.SeleniumJavaScriptExecutor;
import com.applitools.eyes.selenium.positioning.ScrollPositionProvider;
import com.applitools.utils.ArgumentGuard;
import java.util.List;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps a target locator so we can keep track of which frames have been
 * switched to.
 */
public class EyesWebTargetLocator extends EyesTargetLocator {

    protected Logger logger;
    private final EyesWebDriver driver;
    private final SeleniumJavaScriptExecutor jsExecutor;
    private final ScrollPositionProvider scrollPosition;
    protected WebDriver.TargetLocator targetLocator;


    /**
     * Initialized a new EyesWebTargetLocator object.
     * @param driver        The WebDriver from which the targetLocator was received.
     * @param targetLocator The actual TargetLocator object.
     */
    public EyesWebTargetLocator(Logger logger, EyesWebDriver driver,
        WebDriver.TargetLocator targetLocator) {
        super(logger, targetLocator);
        ArgumentGuard.notNull(driver, "driver");
        this.driver = driver;
        this.jsExecutor = new SeleniumJavaScriptExecutor(driver);
        this.scrollPosition = new ScrollPositionProvider(logger, jsExecutor);
    }



    protected EyesWebDriver getDriver () { return driver; }
    protected ScrollPositionProvider getScrollProvider() { return scrollPosition; }

    protected WebElement getFrameByIndex(int index) {
        logger.verbose("Getting frames list...");
        List<WebElement> frames = driver.findElementsByCssSelector("frame, iframe");
        if (index > frames.size()) {
            throw new NoSuchFrameException(String.format("Frame index [%d] is invalid!", index));
        }
        logger.verbose("Done! getting the specific frame...");
        return frames.get(index);
    }

    protected List<WebElement> getFramesByName (String name) {
        return driver.findElementsByName(name);
    }

    protected void switchToFrame(String nameOrId) {
        targetLocator.frame(nameOrId);
    }

    protected void switchToFrame(WebElement frame) {
        targetLocator.frame(frame);
    }

    protected void switchToParent() {
        driver.switchTo().parentFrame();
    }

    protected void switchToDefault() {
        driver.switchTo().defaultContent();
    }

    protected void switchToWindow(String nameOrHandle) {
        targetLocator.window(nameOrHandle);
    }

}