package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.Region;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.selenium.frames.Frame;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import com.applitools.eyes.selenium.positioning.SeleniumScrollingPositionProvider;
import com.applitools.utils.ArgumentGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumDriver;
import java.io.IOException;
import javax.annotation.Nullable;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public abstract class AppiumScrollPositionProvider implements SeleniumScrollingPositionProvider {

    protected final Logger logger;
    protected final AppiumDriver driver;
    protected final EyesAppiumDriver eyesDriver;
    protected double distanceRatio;
    protected int verticalScrollGap;

    protected ContentSize contentSize;

    private WebElement firstVisibleChild;
    private boolean isVerticalScrollGapSet;

    public AppiumScrollPositionProvider (Logger logger, EyesAppiumDriver driver) {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(driver, "driver");

        this.logger = logger;
        this.driver = driver.getRemoteWebDriver();
        this.eyesDriver = driver;
        distanceRatio = 0.0;
        verticalScrollGap = 0;
        isVerticalScrollGapSet = false;
    }

    protected WebElement getCachedFirstVisibleChild () {
        WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
        if (firstVisibleChild == null) {
            logger.verbose("Could not find first visible child in cache, getting (this could take a while)");
            firstVisibleChild = EyesAppiumUtils.getFirstVisibleChild(activeScroll);

        }
        return firstVisibleChild;
    }

    @Nullable
    protected ContentSize getCachedContentSize () {
        if (contentSize == null) {
            WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            try {
                contentSize = EyesAppiumUtils.getContentSize(driver, activeScroll);
                logger.verbose("Retrieved contentSize, it is: " + contentSize);
            } catch (IOException e) {
                logger.log("WARNING: could not retrieve content size from active scroll element");
            }
        }
        return contentSize;
    }

    public Location getScrollableViewLocation() {
        logger.verbose("Getting the location of the scrollable view");
        WebElement activeScroll, firstVisChild;
        Point scrollLoc, firstVisChildLoc;
        try {
            activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            firstVisChild = getCachedFirstVisibleChild();
        } catch (NoSuchElementException e) {
            return new Location(0, 0);
        }
        scrollLoc = activeScroll.getLocation();
        firstVisChildLoc = firstVisChild.getLocation();
        logger.verbose("The location of the first visible child is " + firstVisChildLoc);
        if (!isVerticalScrollGapSet) {
            verticalScrollGap = firstVisChildLoc.y - scrollLoc.y;
            isVerticalScrollGapSet = true;
        }
        Location loc = new Location(scrollLoc.x, scrollLoc.y + verticalScrollGap);
        logger.verbose("The location of the scrollable view is " + loc + ", accounting for a " +
            "vertical scroll gap of " + verticalScrollGap);
        return loc;
    }

    public Region getScrollableViewRegion() {
        logger.verbose("Getting the region of the scrollable view");
        WebElement activeScroll;
        Region reg;
        try {
            activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            Location scrollLoc = getScrollableViewLocation();
            Dimension scrollDim = activeScroll.getSize();
            reg = new Region(scrollLoc.getX(), scrollLoc.getY(), scrollDim.width, scrollDim.height - verticalScrollGap);
        } catch (NoSuchElementException e) {
            logger.verbose("WARNING: couldn't find scrollview, returning empty Region");
            reg =new Region(0, 0, 0, 0);
        }
        logger.verbose("The region of the scrollable view is " + reg + ", accounting for a vertical " +
            "scroll gap of " + verticalScrollGap);
        return reg;
    }

    /**
     * @return The scroll position of the current frame.
     */
    public Location getCurrentPosition(boolean absolute) {
        logger.verbose("AppiumScrollPositionProvider - getCurrentPosition()");
        Location loc = getScrollableViewLocation();
        Point childLoc = getCachedFirstVisibleChild().getLocation();
        logger.verbose("The first visible child is at " + childLoc);
        Location pos;
        if (absolute) {
            pos = new Location(loc.getX() * 2 - childLoc.x, loc.getY() * 2 - childLoc.y);
        } else {
            // the position of the scrollview is basically the offset of the first visible child
            pos = new Location(loc.getX() - childLoc.x, loc.getY() - childLoc.y);
        }
        logger.verbose("The current scroll position is " + pos);
        return pos;
    }

    public Location getCurrentPosition() {
        return getCurrentPosition(false);
    }

    public abstract void setPosition(Location location);

    public abstract void setPosition(WebElement element);

    public void setPosition(Frame frame) {
        setPosition(frame.getReference());
    }

    /**
     *
     * @return The entire size of the container which the position is relative
     * to.
     */
    public RectangleSize getEntireSize() {
        try {
            int windowHeight = driver.manage().window().getSize().getHeight();
            ContentSize contentSize = getCachedContentSize();
            int scrollContentHeight = contentSize.getScrollContentHeight();
            int outsideScrollviewHeight = windowHeight - contentSize.height;
            RectangleSize result = new RectangleSize(contentSize.width,
                scrollContentHeight + outsideScrollviewHeight + verticalScrollGap);
            logger.verbose("AppiumScrollPositionProvider - Entire size: " + result + " (Accounting for " +
                "a vertical scroll gap of " + verticalScrollGap + ", with a scroll content height of " +
                scrollContentHeight + ")");
            return result;
        } catch (NoSuchElementException e) {
            logger.verbose("Could not retrieve the first scrollable view, looks like there isn't one. " +
                           "Will return the viewport size instead.");
            return eyesDriver.getDefaultContentViewportSize();
        }
    }

    public PositionMemento getState() {
        return new ScrollPositionMemento(getCurrentPosition());
    }

    public abstract void restoreState(PositionMemento state);

    public void scrollToBottomRight() {
        setPosition(new Location(9999999, 9999999));
    }

    public abstract Location scrollDown(boolean returnAbsoluteLocation);

}
