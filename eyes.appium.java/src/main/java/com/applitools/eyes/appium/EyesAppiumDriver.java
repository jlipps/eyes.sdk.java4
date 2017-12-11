package com.applitools.eyes.appium;

import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import io.appium.java_client.AppiumDriver;
import java.util.HashMap;
import org.openqa.selenium.Capabilities;

public class EyesAppiumDriver extends EyesWebDriver {


  public EyesAppiumDriver(Logger logger, Eyes eyes, AppiumDriver driver) {
    super(logger, eyes, driver);
  }

  /**
   * @param forceQuery If true, we will perform the query even if we have a cached viewport size.
   * @return The viewport size of the default content (outer most frame).
   */
  public RectangleSize getDefaultContentViewportSize(boolean forceQuery) {
    logger.verbose("getDefaultContentViewportSize(forceQuery: " + forceQuery + ")");

    if (defaultContentViewportSize != null && !forceQuery) {
      logger.verbose("Using cached viewport size: " + defaultContentViewportSize);
      return defaultContentViewportSize;
    }

    logger.verbose("Retrieving session capabilities to get viewport size...");
    Capabilities caps;
    try {
      // TODO figure out why this is throwing NPE
      caps = driver.getCapabilities();
    } catch (Exception e) {
      logger.verbose(e.toString());
      return new RectangleSize(0, 0);
    }
    logger.verbose("Extracting viewport size from capabilities...");
    logger.verbose((String) caps.getCapability("viewportRect"));
    HashMap<String, Integer> rectMap = (HashMap<String, Integer>) caps.getCapability("viewportRect");
    defaultContentViewportSize = new RectangleSize(rectMap.get("width"), rectMap.get("height"));
    logger.verbose("Done! Viewport size: " + defaultContentViewportSize);

    return defaultContentViewportSize;
  }

}
