package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import java.rmi.Remote;
import java.time.Duration;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public class AndroidScrollPositionProvider extends AppiumScrollPositionProvider {

    private Location curScrollPos;
    private int curScrollPageIndex;
    private int curScrollItemIndex;
    private int numFullPages;
    private int lastScrollAmount;

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
            ContentSize contentSize = getCachedContentSize();
            int overflow = contentSize.scrollableOffset;
            if (overflow > 0) {
                numFullPages += Math.floor(overflow / contentSize.height);
                lastScrollAmount = overflow - ((numFullPages - 1) * contentSize.height);
            }
        } catch (Exception e) {
            logger.verbose("Could not get first scrollable view or its contentSize");
        }
    }

    @Override
    public Location getScrollableViewLocation() {
        logger.verbose("Getting the location of the scrollable view");
        WebElement activeScroll;
        try {
            activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
        } catch (NoSuchElementException e) {
            return new Location(0, 0);
        }
        Point scrollLoc = activeScroll.getLocation();
        Location loc = new Location(scrollLoc.x, scrollLoc.y);
        logger.verbose("The location of the scrollable view is " + loc);
        return loc;
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
            EyesAppiumUtils.scrollBackToElement((AndroidDriver) driver, (RemoteWebElement) activeScroll,
                (RemoteWebElement) element);
        } catch (NoSuchElementException e) {
            logger.verbose("Could not set position because there was no scrollable view; doing nothing");
        }
    }

    public void restoreState(PositionMemento state) {
        setPosition(new Location(((ScrollPositionMemento) state).getX(), ((ScrollPositionMemento) state).getY()));
    }

    public Location scrollDown(boolean returnAbsoluteLocation) {
        if (contentSize == null) {
            logger.verbose("About to scroll but had no content size set; setting it");
            resetScrollableViewData();
        }
        // for some reason we need a bit of extra touch padding otherwise the scroll doesn't happen
        // this number is the smallest that worked, but may not be correct for all device types
        // and apps
        // FIXME investigate a better option
        int magicExtraPadding = 9;

        int extraPadding = (int) (contentSize.height * 0.1);
        int startX = contentSize.left + (contentSize.width / 2);
        int startY = contentSize.top + contentSize.height - contentSize.touchPadding - extraPadding;
        int endX = startX;
        int endY = contentSize.top + contentSize.touchPadding + extraPadding;

        TouchAction scrollAction = new TouchAction(driver);
        scrollAction.press(startX, startY).waitAction(Duration.ofMillis(1500));
        scrollAction.moveTo(endX, endY);
        scrollAction.release();
        driver.performTouchAction(scrollAction);

        // because Android scrollbars are visible a bit after touch, we should wait for them to
        // disappear before handing control back to the screenshotter
        try { Thread.sleep(750); } catch (InterruptedException ign) {}

        LastScrollData lastScrollData = EyesAppiumUtils.getLastScrollData(driver);
        logger.verbose("After scroll lastScrollData was: " + lastScrollData);
        if (lastScrollData == null) {
            // if we didn't get last scroll data, it should be because we were already at the end of
            // the scroll view, so just return the same scroll position as last time to say in effect
            // that we did not scroll. It could also be because something goofed in Android and no
            // scroll data was generated. In that case we're just screwed and our picture will
            // be truncated. Unfortunately there's no way to tell the difference between the usual
            // case and the error case so we can't provide any better feedback
            logger.verbose("Did not get last scroll data; assume there was no more scroll");
            return curScrollPos;
        }

        if (lastScrollData.scrollX != -1 && lastScrollData.scrollY != -1) {
            // if we got scrolldata from a ScrollView (not List or Grid), actively set the scroll
            // position with correct x/y values
            curScrollPos = new Location(lastScrollData.scrollX, lastScrollData.scrollY);
            return curScrollPos;
        }


        // otherwise, fake x/y coords by doing some math on item count and index
        curScrollItemIndex = lastScrollData.toIndex;
        double avgItemHeight = contentSize.scrollableOffset / lastScrollData.itemCount;
        curScrollPos = new Location(0, (int) avgItemHeight * curScrollItemIndex);
        logger.verbose("Did not get actual x/y coords from lastScrollData, so estimating that " +
            "current scroll position is " + curScrollPos + ", based on item count of " +
            lastScrollData.itemCount + ", avg item height of " + avgItemHeight + ", and scrolled-to " +
            "index of " + curScrollItemIndex);

        return curScrollPos;
    }

}
