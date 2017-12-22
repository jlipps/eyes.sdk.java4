package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.Region;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.positioning.ScrollingPositionProvider;
import com.applitools.eyes.selenium.frames.Frame;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import com.applitools.eyes.selenium.positioning.SeleniumScrollingPositionProvider;
import com.applitools.utils.ArgumentGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.swing.text.AbstractDocument.Content;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.parser.JSONParser;
import jdk.nashorn.internal.runtime.Context;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.json.Json;

public class AppiumScrollPositionProvider implements SeleniumScrollingPositionProvider {

    private static final String SCROLL_DIRECTION_UP = "up";
    private static final String SCROLL_DIRECTION_DOWN = "down";
    private static final String SCROLL_DIRECTION_LEFT = "left";
    private static final String SCROLL_DIRECTION_RIGHT = "right";

    protected final Logger logger;
    protected final AppiumDriver driver;
    protected final EyesAppiumDriver eyesDriver;
    private WebElement firstVisibleChild;
    private ObjectMapper objectMapper;

    public AppiumScrollPositionProvider (Logger logger, EyesAppiumDriver driver) {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(driver, "driver");

        this.logger = logger;
        this.driver = driver.getRemoteWebDriver();
        this.eyesDriver = driver;
        objectMapper = new ObjectMapper();
    }

    private WebElement getCachedFirstVisibleChild () {
        WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
        if (firstVisibleChild == null) {
            logger.verbose("Could not find first visible child in cache, getting (this could take a while)");
            firstVisibleChild = EyesAppiumUtils.getFirstVisibleChild(activeScroll);
        }
        return firstVisibleChild;
    }

    public Location getScrollableViewLocation() {
        WebElement activeScroll;
        try {
            activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            Point scrollLoc = activeScroll.getLocation();
            return new Location(scrollLoc.x, scrollLoc.y);
        } catch (NoSuchElementException e) {
            return new Location(0, 0);
        }
    }

    public Region getScrollableViewRegion() {
        WebElement activeScroll;
        try {
            activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            Point scrollLoc = activeScroll.getLocation();
            Rectangle scrollDim = activeScroll.getRect();
            return new Region(scrollLoc.x, scrollLoc.y, scrollDim.width, scrollDim.height);
        } catch (NoSuchElementException e) {
            logger.verbose("WARNING: couldn't find scrollview, returning empty Region");
            return new Region(0, 0, 0, 0);
        }
    }

    /**
     * @return The scroll position of the current frame.
     */
    public Location getCurrentPosition() {
        logger.verbose("AppiumScrollPositionProvider - getCurrentPosition()");
        WebElement activeScroll;
        try {
            activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
        } catch (NoSuchElementException e) {
            return new Location(0, 0);
        }
        Point loc = activeScroll.getLocation();
        Point childLoc = getCachedFirstVisibleChild().getLocation();
        // the position of the scrollview is basically the offset of the first visible child
        return new Location(loc.x - childLoc.x, loc.y - childLoc.y);
    }

    /**
     * Go to the specified location.
     * @param location The position to scroll to.
     */
    public void setPosition(Location location) {
        logger.log("Warning: Appium cannot reliably scroll based on location; pass an element instead if you can");
        Location curPos = getCurrentPosition();
        logger.verbose("Wanting to scroll to " + location);
        logger.verbose("Current scroll position is " + getCurrentPosition());
        Location lastPos = null;


        HashMap<String, String> args = new HashMap<>();
        String directionY = ""; // empty means we don't have to do any scrolling
        String directionX = "";
        if (curPos.getY() < location.getY()) {
            directionY = SCROLL_DIRECTION_DOWN;
        } else if (curPos.getY() > location.getY()) {
            directionY = SCROLL_DIRECTION_UP;
        }
        if (curPos.getX() < location.getX()) {
            directionX = SCROLL_DIRECTION_RIGHT;
        } else if (curPos.getX() > location.getX()) {
            directionX = SCROLL_DIRECTION_LEFT;
        }


        // first handle any vertical scrolling
        if (directionY != "") {
            logger.verbose("Scrolling to Y component");
            args.put("direction", directionY);
            while ((directionY == SCROLL_DIRECTION_DOWN && curPos.getY() < location.getY()) ||
                (directionY == SCROLL_DIRECTION_UP && curPos.getY() > location.getY())) {
                logger.verbose("Scrolling " + directionY);
                driver.executeScript("mobile: scroll", args);
                lastPos = curPos;
                curPos = getCurrentPosition();
                logger.verbose("Scrolled to " + curPos);
                if (curPos.getY() == lastPos.getY()) {
                    logger.verbose("Ended up at the same place as last scroll, stopping");
                    break;
                }
            }
        }

        // then handle any horizontal scrolling
        if (directionX != "") {
            logger.verbose("Scrolling to X component");
            args.put("direction", directionX);
            while ((directionX == SCROLL_DIRECTION_RIGHT && curPos.getX() < location.getX()) ||
                (directionX == SCROLL_DIRECTION_LEFT && curPos.getX() > location.getX())) {
                logger.verbose("Scrolling " + directionY);
                driver.executeScript("mobile: scroll", args);
                lastPos = curPos;
                curPos = getCurrentPosition();
                if (curPos.getX() == lastPos.getX()) {
                    logger.verbose("Ended up at the same place as last scroll, stopping");
                    break;
                }
            }
        }
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
        WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
        String contentSizeJson = activeScroll.getAttribute("contentSize");
        ContentSize contentSize;
        try {
            contentSize = objectMapper.readValue(contentSizeJson, ContentSize.class);
        } catch (IOException e) {
            logger.verbose("WARNING: could not parse contentSize JSON: " + e);
            return new RectangleSize(0, 0);
        }
        RectangleSize result = new RectangleSize(contentSize.width, contentSize.scrollableOffset);
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
        setPosition(new Location(9999999, 9999999));
    }

    public Location scrollDown() {
        EyesAppiumUtils.scrollByDirection(driver, SCROLL_DIRECTION_DOWN);
        return getCurrentPosition();
    }

}
