package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.positioning.ScrollingPositionProvider;
import com.applitools.eyes.selenium.frames.Frame;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import com.applitools.eyes.selenium.positioning.SeleniumScrollingPositionProvider;
import com.applitools.utils.ArgumentGuard;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import java.util.HashMap;
import java.util.List;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.parser.JSONParser;
import jdk.nashorn.internal.runtime.Context;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.json.Json;

public class AppiumScrollPositionProvider implements SeleniumScrollingPositionProvider {

    protected final Logger logger;
    protected final AppiumDriver driver;
    protected final EyesAppiumDriver eyesDriver;
    private WebElement firstVisibleChild;

    public AppiumScrollPositionProvider (Logger logger, EyesAppiumDriver driver) {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(driver, "driver");

        this.logger = logger;
        this.driver = driver.getRemoteWebDriver();
        this.eyesDriver = driver;
    }

    private WebElement getActiveScrollView () {
        // the 'active' scroll view is either the one added to the chain most recently, or if the chain is empty, the first scroll view we find
        if (eyesDriver.getFrameChain().size() > 0) {
            return eyesDriver.getFrameChain().peek().getReference();
        }
        try {
            return driver.findElementByXPath("//*[@scrollable='true']");
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Tried to get the active scroll view but none was found");
        }
    }

    private WebElement getCachedFirstVisibleChild () {
        WebElement activeScroll = getActiveScrollView();
        if (firstVisibleChild == null) {
            firstVisibleChild = activeScroll.findElement(By.xpath("/*[@firstVisible='true']"));
        }
        return firstVisibleChild;
    }

    /**
     * @return The scroll position of the current frame.
     */
    public Location getCurrentPosition() {
        logger.verbose("AppiumScrollPositionProvider - getCurrentPosition()");
        WebElement activeScroll;
        try {
            activeScroll = getActiveScrollView();
        } catch (NoSuchElementException e) {
            return new Location(0, 0);
        }
        Point loc = activeScroll.getLocation();
        Point childLoc = getCachedFirstVisibleChild().getLocation();
        // the position of the scrollview is basically the offset of the first visible child
        return new Location(childLoc.x - loc.x, childLoc.y - loc.y);
    }

    /**
     * Go to the specified location.
     * @param location The position to scroll to.
     */
    public void setPosition(Location location) {
        logger.log("Warning: Appium cannot reliably scroll based on location; pass an element instead");
    }

    public void setPosition(WebElement element) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("toVisible", "true");
        params.put("element", element);
        driver.executeScript("mobile: scroll", params);
    }

    public void setPosition(Frame frame) {
        setPosition(frame.getReference());
    }

    /**
     *
     * @return The entire size of the container which the position is relative
     * to.
     */
    public RectangleSize getEntireSize() {
        WebElement activeScroll = getActiveScrollView();
        String contentSizeList = activeScroll.getAttribute("contentSize");
        // a string that looks something like width=10,height=20,top=0,left=0,scrollableOffset=1000
        String[] attrs = contentSizeList.split(",");
        HashMap<String, Integer> contentSize = new HashMap<>();
        for (String attr : attrs) {
            String[] keyVal = attr.split("=");
            contentSize.put(keyVal[0], new Integer(keyVal[1]));
        }
        RectangleSize result = new RectangleSize(contentSize.get("width"), contentSize.get("scrollableOffset"));
        logger.verbose("AppiumScrollPositionProvider - Entire size: " + result);
        return result;
    }

    public PositionMemento getState() {
        return new ScrollPositionMemento(getCurrentPosition());
    }

    public void restoreState(PositionMemento state) {
        logger.log("Warning: AppiumScrollPositionProvider cannot reliably restore position state");
    }

    public void scrollToBottomRight() {
        // EyesSeleniumUtils.scrollToBottomRight(this.executor);
    }

}
