package com.applitools.eyes.appium;

import com.applitools.eyes.LogHandler;
import com.applitools.eyes.StdoutLogHandler;
import io.appium.java_client.MobileBy;
import io.appium.java_client.Setting;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class AppiumAndroidFullPageScreenshotTest {

    public static void main(String[] args) throws Exception {

        // Set desired capabilities.
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("deviceName", "Android Emulator");
        capabilities.setCapability("platformVersion", "8.1");
        // TODO do not merge until this is replaced with a non-user-specific path
        capabilities.setCapability("app", "/Users/jlipps/Code/testapps/Applitools-Android-Demo.apk");
        capabilities.setCapability("automationName", "UiAutomator2");
        capabilities.setCapability("newCommandTimeout", 300);

        // Open the app.
        AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);

        driver.manage().timeouts().implicitlyWait(5000, TimeUnit.MILLISECONDS);

        // Initialize the eyes SDK and set your private API key.
        Eyes eyes = new Eyes();
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));

        LogHandler logHandler = new StdoutLogHandler(true);
        eyes.setLogHandler(logHandler);
        eyes.setForceFullPageScreenshot(true);
        eyes.setSaveDebugScreenshots(true);
        eyes.setMatchTimeout(1000);

        try {
            // Start the test.
            eyes.open(driver, "Applitools Demo", "Appium Native Android with Full page screenshot");

            eyes.checkWindow("scroll");

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
