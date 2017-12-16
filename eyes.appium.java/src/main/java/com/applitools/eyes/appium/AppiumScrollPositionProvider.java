package com.applitools.eyes.appium;

import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.positioning.ScrollingPositionProvider;
import com.applitools.eyes.selenium.positioning.ScrollPositionMemento;
import com.applitools.utils.ArgumentGuard;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class AppiumScrollPositionProvider implements ScrollingPositionProvider {

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
    logger.verbose("ScrollPositionProvider - getCurrentPosition()");
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
    logger.verbose("ScrollPositionProvider - Scrolling to " + location);
//    EyesSeleniumUtils.setCurrentScrollPosition(executor, location);
    logger.verbose("ScrollPositionProvider - Done scrolling!");
  }

  /**
   *
   * @return The entire size of the container which the position is relative
   * to.
   */
  public RectangleSize getEntireSize() {
    RectangleSize result = new RectangleSize(0, 0);
//    result = EyesSeleniumUtils.getCurrentFrameContentEntireSize(executor);
    logger.verbose("ScrollPositionProvider - Entire size: " + result);
    return result;
  }

  public PositionMemento getState() {
    return new ScrollPositionMemento(getCurrentPosition());
  }

  public void restoreState(PositionMemento state) {
    ScrollPositionMemento s = (ScrollPositionMemento) state;
    setPosition(new Location(s.getX(), s.getY()));
  }

  public void scrollToBottomRight() {
    // EyesSeleniumUtils.scrollToBottomRight(this.executor);
  }

}
