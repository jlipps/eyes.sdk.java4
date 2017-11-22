package com.applitools.eyes.selenium;

import com.applitools.eyes.FixedCutProvider;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.selenium.fluent.Target;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DellTest {
    public static void main(String[] args) throws MalformedURLException, URISyntaxException {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("appiumVersion", "1.6.4");
        caps.setCapability("deviceName", "Samsung Galaxy S4 Emulator");
        caps.setCapability("platformName", "Android");
        caps.setCapability("platformVersion", "4.4");
        caps.setCapability("browserName", "Browser");
        caps.setCapability("username", "matan");
        caps.setCapability("accesskey", "ec79e940-078b-41d4-91a6-d7d6008cf1ea");

        RemoteWebDriver driver = new RemoteWebDriver(new URL("http://ondemand.saucelabs.com/wd/hub"), caps);
        driver.get("https://dell.com");

        Eyes eyes = new Eyes();
        eyes.setServerUrl(new URI("https://localhost.applitools.com"));
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        eyes.setForceFullPageScreenshot(true);

        eyes.setSaveDebugScreenshots(true);
        eyes.setDebugScreenshotsPath("c:\\temp\\logs");
        eyes.setDebugScreenshotsPrefix("Dell");

        eyes.setImageCut(new FixedCutProvider(77, 0, 0, 0));

        try
        {
            eyes.open(driver, "dell", "Sauce dell website android");

            eyes.setDebugScreenshotsPrefix("Dell_Fluent_");
            eyes.check("test", Target.window().fully());

            eyes.setDebugScreenshotsPrefix("Dell_Classic_");
            eyes.checkWindow("test");

            TestResults tr = eyes.close();
        }
        finally
        {
            eyes.abortIfNotClosed();
            driver.quit();
        }
    }
}
