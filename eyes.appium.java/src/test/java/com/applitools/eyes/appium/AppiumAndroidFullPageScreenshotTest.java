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
//        capabilities.setCapability("app", "/Users/jlipps/Code/testapps/ApiDemos-debug.apk");
//        capabilities.setCapability("appPackage", "io.appium.android.apis");
//        capabilities.setCapability("appActivity", "io.appium.android.apis.view.Grid2");
//        capabilities.setCapability("appActivity", "io.appium.android.apis.view.List1");
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
        eyes.setDebugScreenshotsPath("/Users/jlipps/Desktop/eyes-screens");
        eyes.setMatchTimeout(1000);
        eyes.setStitchOverlap(44);

        try {
            // Start the test.
            eyes.open(driver, "ApiDemos", "Appium Native Android with Full page screenshot");

            WebElement scroller = driver.findElement(By.xpath("//*[@scrollable='true']"));
            //System.out.println(scroller.getAttribute("className"));

//            System.out.println(scroller.getAttribute("contentSize"));

//

//
//            TouchAction scrollAction = new TouchAction(driver);
//            scrollAction.press(500, 1800).waitAction(Duration.ofMillis(1000));
//            scrollAction.moveTo(500, 600);//.waitAction(Duration.ofMillis(2500));
//            scrollAction.release();
//            driver.performTouchAction(scrollAction);
//
//
//            Thread.sleep(2000);
//            System.out.println(scroller.getAttribute("contentSize"));
//
//            scrollAction = new TouchAction(driver);
//            scrollAction.press(500, 600).waitAction(Duration.ofMillis((1000)));
//            scrollAction.moveTo(500, 1800);//.waitAction(Duration.ofMillis((2500)));
//            scrollAction.release();
//            driver.performTouchAction(scrollAction);
//
//            Thread.sleep(2000);
//            System.out.println(scroller.getAttribute("contentSize"));
//            System.out.println(driver.getSessionDetail("lastScrollData"));

//            WebElement scroller = driver.findElement(MobileBy.xpath("//*[@scrollable='true']"));
//            WebElement firstVis = scroller.findElement(MobileBy.xpath("/*[@firstVisible='true']"));
//            System.out.println(firstVis.getLocation());
//            TouchAction scrollAction = new TouchAction(driver);
//            scrollAction.press(50, 1200).waitAction(Duration.ofMillis(2500));
//            scrollAction.moveTo(0, 600);
//            scrollAction.release();
//            driver.performTouchAction(scrollAction);
//            System.out.println(firstVis.getLocation());

            eyes.checkWindow("apidemos list");

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
