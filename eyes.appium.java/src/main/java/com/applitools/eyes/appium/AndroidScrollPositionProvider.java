package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import java.time.Duration;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public class AndroidScrollPositionProvider extends AppiumScrollPositionProvider {

    private Location curScrollPos;
    private Location scrollableViewLoc;

    public AndroidScrollPositionProvider(Logger logger, EyesAppiumDriver driver) {
        super(logger, driver);
    }

    @Override
    public Location getScrollableViewLocation() {
        logger.verbose("Getting the location of the scrollable view");
        if (scrollableViewLoc == null) {
            WebElement activeScroll;
            try {
                activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            } catch (NoSuchElementException e) {
                return new Location(0, 0);
            }
            Point scrollLoc = activeScroll.getLocation();
            scrollableViewLoc = new Location(scrollLoc.x, scrollLoc.y);
        }
        logger.verbose("The location of the scrollable view is " + scrollableViewLoc);
        return scrollableViewLoc;
    }

    @Override
    public Location getCurrentPosition(boolean absolute) {
        logger.verbose("AndroidScrollPositionProvider - getCurrentPosition()");
        Location loc = getScrollableViewLocation();
        if (curScrollPos == null) {
            logger.verbose("There was no current scroll position registered, so setting it for the first time");
            ContentSize contentSize = getCachedContentSize();
            LastScrollData scrollData = EyesAppiumUtils.getLastScrollData(driver);
            logger.verbose("Last scroll data from the server was: " + scrollData);
            curScrollPos = getScrollPosFromScrollData(contentSize, scrollData);
        }
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

        if (location.getY() == curScrollPos.getY() && location.getX() == curScrollPos.getX()) {
            logger.log("Already at the desired position, doing nothing");
            return;
        } else {
            logger.verbose(
                "Setting position to 0, 0 by scrolling all the way back to the top");
            Location lastScrollPos = curScrollPos;
            while (curScrollPos.getY() > 0) {
                scroll(false);
                if (lastScrollPos.getY() == curScrollPos.getY()) {
                    // if we wound up in the same place after a scroll, abort
                    break;
                }
                lastScrollPos = curScrollPos;
            }
        }
    }

    public void setPosition(WebElement element) {
        logger.log("Warning: can only scroll back to elements that have already been seen");
        try {
            WebElement activeScroll = EyesAppiumUtils.getFirstScrollableView(driver);
            EyesAppiumUtils.scrollBackToElement((AndroidDriver) driver, (RemoteWebElement) activeScroll,
                (RemoteWebElement) element);

            LastScrollData lastScrollData = EyesAppiumUtils.getLastScrollData(driver);
            logger.verbose("After scrolling back to first child, lastScrollData was: " + lastScrollData);
            curScrollPos = new Location(lastScrollData.scrollX, lastScrollData.scrollY);
        } catch (NoSuchElementException e) {
            logger.verbose("Could not set position because there was no scrollable view; doing nothing");
        }
    }

    public void restoreState(PositionMemento state) {
        setPosition(new Location(((ScrollPositionMemento) state).getX(), ((ScrollPositionMemento) state).getY()));
    }

    private void scroll(boolean isDown) {
        ContentSize contentSize = getCachedContentSize();
        int extraPadding = (int) (contentSize.height * 0.15); // scroll less than the max
        int startX = contentSize.left + (contentSize.width / 2);
        int startY = contentSize.top + contentSize.height - contentSize.touchPadding - extraPadding;
        int endX = startX;
        int endY = contentSize.top + contentSize.touchPadding + extraPadding;

        // if we're scrolling up, just switch the Y vars
        if (!isDown) {
            int temp = endY;
            endY = startY;
            startY = temp;
        }

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
        curScrollPos = getScrollPosFromScrollData(contentSize, lastScrollData);
    }

    public Location scrollDown(boolean returnAbsoluteLocation) {
        scroll(true);
        return getCurrentPosition(returnAbsoluteLocation);
    }

    private Location getScrollPosFromScrollData(ContentSize contentSize, LastScrollData scrollData) {
        logger.verbose("Getting scroll position from last scroll data (" + scrollData + ") and " +
            "contentSize (" + contentSize + ")");
        if (scrollData == null) {
            // if we didn't get last scroll data, it should be because we were already at the end of
            // the scroll view, so just return the same scroll position as last time to say in effect
            // that we did not scroll. It could also be because something goofed in Android and no
            // scroll data was generated. In that case we're just screwed and our picture will
            // be truncated. Unfortunately there's no way to tell the difference between the usual
            // case and the error case so we can't provide any better feedback
            logger.verbose("Did not get last scroll data; assume there was no more scroll");
            return curScrollPos;
        }

        if (scrollData.scrollX != -1 && scrollData.scrollY != -1) {
            // if we got scrolldata from a ScrollView (not List or Grid), actively set the scroll
            // position with correct x/y values
            return new Location(scrollData.scrollX, scrollData.scrollY);
        }

        // otherwise, fake x/y coords by doing some math on item count and index
        double avgItemHeight = contentSize.scrollableOffset / scrollData.itemCount;
        Location pos = new Location(0, (int) avgItemHeight * scrollData.toIndex);
        logger.verbose("Did not get actual x/y coords from lastScrollData, so estimating that " +
            "current scroll position is " + pos + ", based on item count of " +
            scrollData.itemCount + ", avg item height of " + avgItemHeight + ", and scrolled-to " +
            "index of " + scrollData.toIndex);
        return pos;
    }

}
