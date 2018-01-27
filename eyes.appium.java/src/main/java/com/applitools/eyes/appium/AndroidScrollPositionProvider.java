package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import java.io.IOException;
import java.time.Duration;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

public class AndroidScrollPositionProvider extends AppiumScrollPositionProvider {

    private Location curScrollPos;
    private int curScrollPageIndex;
    private int curScrollItemIndex;
    private int numFullPages;
    private int lastScrollAmount;
    private ContentSize scrollContentSize;

    public AndroidScrollPositionProvider(Logger logger, EyesAppiumDriver driver) {
        super(logger, driver);
        resetScrollTracking();
    }

    private void resetScrollTracking() {
        logger.verbose("Resetting current scroll position and current scroll page");
        // unfortunately we have to assume that the current scroll position of the main scroll view
        // starts at 0, 0; there is no way to get the current scroll position accurately from android
        curScrollPos = new Location(0, 0);
        curScrollPageIndex = 0;
    }

    private void resetScrollableViewData() {
        logger.verbose("Resetting scrollable view data based on first scrollable view");
        numFullPages = 1;
        lastScrollAmount = 0;
        try {
            WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            scrollContentSize = EyesAppiumUtils.getContentSize(driver, activeScroll);
            int overflow = scrollContentSize.scrollableOffset;
            if (overflow > 0) {
                numFullPages += Math.floor(overflow / scrollContentSize.height);
                lastScrollAmount = overflow - ((numFullPages - 1) * scrollContentSize.height);
            }
        } catch (Exception e) {
            logger.verbose("Could not get first scrollable view or its contentSize");
        }
    }

    protected int calcEntireContentHeight (ContentSize contentSize) {
        logger.verbose("Content size height is " + contentSize.height + " and s.o. is " + contentSize.scrollableOffset);
        return contentSize.height + contentSize.scrollableOffset;
    }

    @Override
    public Location getCurrentPosition(boolean absolute) {
        logger.verbose("AndroidScrollPositionProvider - getCurrentPosition()");
        Location loc = getScrollableViewLocation();
        Location pos;
        if (absolute) {
            pos = new Location(loc.getX() + curScrollPos.getX(), loc.getY() + curScrollPos.getY());
        } else {
            pos = new Location(curScrollPos.getX(), curScrollPos.getY());
        }
        logger.verbose("The current scroll position is " + pos);
        return pos;
    }

    public void setPosition(Location location) {
        if (location.getY() != 0 && location.getX() != 0) {
            logger.log("Warning: tried to set position on an Android scroll view, which is not possible");
            return;
        }
        // if we set the position to [0, 0], then take that as a sign that we should reset curScrollPos
        // and curScrollPageIndex, and trigger a refresh of contentSize
        if (location.getY() == curScrollPos.getY() && location.getX() == curScrollPos.getX()) {
            logger.log("Already at the desired position, doing nothing");
            return;
        } else {
            logger.verbose(
                "Setting position to 0, 0 by scrolling all the way back to the first visible element");
            setPosition(getCachedFirstVisibleChild());
        }
        resetScrollTracking();
        resetScrollableViewData();
    }

    public void setPosition(WebElement element) {
        logger.log("Warning: can only scroll back to elements that have already been seen");
        try {
            WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            EyesAppiumUtils.scrollBackToElement((AndroidDriver) driver, activeScroll, element);
        } catch (NoSuchElementException e) {
            logger.verbose("Could not set position because there was no scrollable view; doing nothing");
        }
    }

    public void restoreState(PositionMemento state) {
        setPosition(new Location(((ScrollPositionMemento) state).getX(), ((ScrollPositionMemento) state).getY()));
    }

//    public Location scrollDown(boolean returnAbsoluteLocation) {
//        int startX = scrollContentSize.left + (scrollContentSize.width / 2);
//        int startY = scrollContentSize.top + scrollContentSize.height - 1;
//        int changeX = 0;
//        int changeY = 0;
//        if (curScrollPageIndex + 1 < numFullPages) {
//            // scroll a full page
//            // TODO generalize this for horizontal scrolling too
//            changeY = -1 * (scrollContentSize.height - scrollContentSize.touchPadding);
//            curScrollPageIndex += 1;
//        } else if (curScrollPageIndex + 1 == numFullPages) {
//            // we've already scrolled all the full pages, so just scroll the last amount
//            changeY = -1 * lastScrollAmount;
//            curScrollPageIndex += 1;
//        }
//        if (changeX != 0 || changeY != 0) {
//            TouchAction scrollAction = new TouchAction(driver);
//            scrollAction.press(startX, startY);
//            scrollAction.moveTo(changeX, changeY).waitAction(Duration.ofMillis(1000));
//            scrollAction.release();
//            driver.performTouchAction(scrollAction);
//            curScrollPos.offset(changeX, changeY);
//        }
//        return curScrollPos;
//    }

    public Location scrollDown(boolean returnAbsoluteLocation) {
        if (scrollContentSize == null) {
            logger.verbose("About to scroll but had no content size set; setting it");
            resetScrollableViewData();
        }
        int startX = scrollContentSize.left + (scrollContentSize.width / 2);
        int startY = scrollContentSize.top + scrollContentSize.height - scrollContentSize.touchPadding - 1;
        int endX = startX;
        int endY = scrollContentSize.top + scrollContentSize.touchPadding;

        TouchAction scrollAction = new TouchAction(driver);
        scrollAction.press(startX, startY).waitAction(Duration.ofMillis(1000));
        scrollAction.moveTo(endX, endY);
        scrollAction.release();
        driver.performTouchAction(scrollAction);

        LastScrollData lastScrollData = EyesAppiumUtils.getLastScrollData(driver);
        if (lastScrollData != null) {
            if (lastScrollData.scrollX != -1 && lastScrollData.scrollY != -1) {
                curScrollPos = new Location(lastScrollData.scrollX, lastScrollData.scrollY);
                return curScrollPos;
            } else {
                if (lastScrollData.toIndex == curScrollItemIndex) {
                    // if we get here, we know that the last scroll didn't do anything so we should
                    // just return the same scroll position as last time
                    return curScrollPos;
                }
            }
        }
        // otherwise, fake it till you make it and just assume we scrolled exactly as much as we wanted
        curScrollPos = curScrollPos.offset(endX - startX, endY - startY);
        return curScrollPos;
    }

}
