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

public class AppiumScrollPositionProvider implements ScrollingPositionProvider {

  protected final Logger logger;
  protected final AppiumDriver driver;

  public AppiumScrollPositionProvider (Logger logger, EyesAppiumDriver driver) {
    ArgumentGuard.notNull(logger, "logger");
    ArgumentGuard.notNull(driver, "driver");

    this.logger = logger;
    this.driver = driver.getRemoteWebDriver();
  }

  /**
   * @return The scroll position of the current frame.
   */
  public Location getCurrentPosition() {
    logger.verbose("ScrollPositionProvider - getCurrentPosition()");
    Location result = new Location(0, 0);
//    try {
//      result = EyesSeleniumUtils.getCurrentScrollPosition(executor);
//    } catch (WebDriverException e) {
//      throw new EyesDriverOperationException("Failed to extract current scroll position!", e);
//    }
//    logger.verbose("Current position: " + result);
    return result;
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
