/*
 * Applitools software.
 */
package com.applitools.eyes.appium;

import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.EyesSeleniumUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public class EyesAppiumUtils extends EyesSeleniumUtils{

    private static String SCROLLVIEW_XPATH = "//*[@scrollable='true']";
    private static String FIRST_VIS_XPATH = "/*[@firstVisible='true']";

    /**
     * @param driver The driver for which to check if it represents a mobile device.
     * @return {@code true} if the platform running the test is a mobile
     * platform. {@code false} otherwise.
     */
    public static boolean isMobileDevice(WebDriver driver) {
        driver = getUnderlyingDriver(driver);
        return driver instanceof AppiumDriver;
    }

    /**
     * @param driver The driver to test.
     * @return {@code true} if the driver is an Android driver.
     * {@code false} otherwise.
     */
    public static boolean isAndroid(WebDriver driver) {
        driver = getUnderlyingDriver(driver);
        return driver instanceof AndroidDriver;
    }

    /**
     * @param driver The driver to test.
     * @return {@code true} if the driver is an iOS driver.
     * {@code false} otherwise.
     */
    public static boolean isIOS(WebDriver driver) {
        driver = getUnderlyingDriver(driver);
        return driver instanceof IOSDriver;
    }

    /**
     * @param driver The driver to get the platform version from.
     * @return The platform version or {@code null} if it is undefined.
     */
    public static String getPlatformVersion(HasCapabilities driver) {
        Capabilities capabilities = driver.getCapabilities();
        Object platformVersionObj =
                capabilities.getCapability
                        (MobileCapabilityType.PLATFORM_VERSION);

        return platformVersionObj == null ?
                null : String.valueOf(platformVersionObj);
    }

    public static WebElement getFirstScrollableView(WebDriver driver) {
        return driver.findElement(By.xpath(SCROLLVIEW_XPATH));
    }

    public static WebElement getFirstVisibleChild(WebElement element) {
        return element.findElement(By.xpath(FIRST_VIS_XPATH));
    }

    public static void scrollByDirection(AppiumDriver driver, String direction) {
        EyesAppiumUtils.scrollByDirection(driver, direction, 1.0);
    }

    public static void scrollByDirection(AppiumDriver driver, String direction, double distanceRatio) {
        HashMap<String, String> args = new HashMap<>();
        args.put("direction", direction);
        args.put("distance", Double.toString(distanceRatio));
        driver.executeScript("mobile: scroll", args);
    }

    public static void scrollBackToElement(AndroidDriver driver, RemoteWebElement scroller, RemoteWebElement scrollToEl) {
        HashMap<String, String> args = new HashMap<>();
        args.put("elementId", scroller.getId());
        args.put("elementToId", scrollToEl.getId());
        driver.executeScript("mobile: scrollBackTo", args);
    }

    public static ContentSize getContentSize(AppiumDriver driver, WebElement element) throws IOException {
        String contentSizeJson = element.getAttribute("contentSize");
        driver.getClass();
        ContentSize contentSize;
        ObjectMapper objectMapper = new ObjectMapper();
        contentSize = objectMapper.readValue(contentSizeJson, ContentSize.class);
        contentSize.setDriver(driver);
        return contentSize;
    }

    @Nullable
    public static LastScrollData getLastScrollData(AppiumDriver driver) {
        Map<String, Long> scrollData = (Map<String, Long>) driver.getSessionDetail("lastScrollData");
        if (scrollData == null) {
            return null;
        }
        return new LastScrollData(scrollData);
    }
}
