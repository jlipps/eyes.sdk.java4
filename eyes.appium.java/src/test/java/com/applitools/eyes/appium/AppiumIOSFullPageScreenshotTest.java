package com.applitools.eyes.appium;

import com.applitools.eyes.LogHandler;
import com.applitools.eyes.StdoutLogHandler;
import io.appium.java_client.MobileBy;
import io.appium.java_client.ios.IOSDriver;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

public class AppiumIOSFullPageScreenshotTest {
  public static void main(String[] args) throws Exception {

    // Set desired capabilities.
    DesiredCapabilities capabilities = new DesiredCapabilities();

    capabilities.setCapability("platformName", "iOS");
    capabilities.setCapability("deviceName", "iPhone 8 Plus");
    capabilities.setCapability("platformVersion", "11.1");
    // TODO do not merge until this is replaced with a non-user-specific path
    capabilities.setCapability("app", "/Users/jlipps/Code/testapps/AppliTools-XCUI-Demo.app");
    capabilities.setCapability("useNewWDA", true);

    // Open the app.
    IOSDriver driver = new IOSDriver<>(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);

    // Initialize the eyes SDK and set your private API key.
    Eyes eyes = new Eyes();
    eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));

    LogHandler logHandler = new StdoutLogHandler(true);
    eyes.setLogHandler(logHandler);
    eyes.setForceFullPageScreenshot(true);
    eyes.setSaveDebugScreenshots(true);
    eyes.setDebugScreenshotsPath("/Users/jlipps/Desktop");

    try {

      // Start the test.
      eyes.open(driver, "Applitools Demo App", "Appium Native iOS with Full page screenshot");
      driver.findElement(MobileBy.AccessibilityId("show elements")).click();
      eyes.checkWindow("Big Table");

      // End the test.
      eyes.close();

    } finally {

      // Close the app.
      driver.quit();

      // If the test was aborted before eyes.close was called, ends the test as aborted.
      eyes.abortIfNotClosed();

    }

  }
}
