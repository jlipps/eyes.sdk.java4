package com.applitools.eyes.selenium;

import com.applitools.eyes.LogHandler;
import com.applitools.eyes.StdoutLogHandler;
import org.openqa.selenium.WebDriver;
import com.applitools.eyes.RectangleSize;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.MalformedURLException;

public class LykosTest {

    public static void main(String[] args) throws MalformedURLException {


        // Initialize the eyes SDK and set your private API key.
        Eyes eyes = new Eyes();
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        eyes.setForceFullPageScreenshot(true);
        eyes.setStitchMode(StitchMode.CSS);
        eyes.setHideScrollbars(false);

        /*
        String sauceLabsKey = "ec79e940-078b-41d4-91a6-d7d6008cf1ea";
        String sauceUrl = String.format("http://%s:%s@ondemand.saucelabs.com:80/wd/hub", "matan", sauceLabsKey);

        DesiredCapabilities caps = DesiredCapabilities.chrome();
        caps.setCapability("platform", "Windows 10");
        caps.setCapability("version", "60.0");
        caps.setCapability("screenResolution", "1600x1200");
        WebDriver driver = new RemoteWebDriver(new URL(sauceUrl), caps);
         */

        LogHandler logHandler = new StdoutLogHandler(true);
        eyes.setLogHandler(logHandler);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("disable-infobars");
        WebDriver driver = new ChromeDriver(options);

        eyes.setDebugScreenshotsPath("c:\\temp\\logs");
        eyes.setDebugScreenshotsPrefix("java_lykos_");
        eyes.setSaveDebugScreenshots(true);

        try {
            driver.get("https://www.lykos.in/");

            eyes.open(driver, "Lykos_test_Sauce", "Lykos", new RectangleSize(1366, 600));

            // Visual checkpoint #1.
            eyes.checkWindow("With hide scroll bar");

            // End the test.
            eyes.close();

        } finally {

            // Close the browser.
            driver.quit();

            // If the test was aborted before eyes.close was called, ends the test as aborted.
            eyes.abortIfNotClosed();
        }

    }
}