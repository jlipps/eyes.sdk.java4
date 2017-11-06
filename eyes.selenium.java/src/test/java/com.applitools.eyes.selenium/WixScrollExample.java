package com.applitools.eyes.selenium;

import com.applitools.eyes.selenium.Eyes;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URI;
import java.net.URISyntaxException;

public final class WixScrollExample {

    private WixScrollExample() {
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("test-type", "start-maximized", "disable-popup-blocking", "disable-infobars");

        WebDriver driver = new ChromeDriver(options);
        Eyes eyes = new Eyes();
        eyes.setServerUrl(URI.create("https://localhost.applitools.com"));
        eyes.setMatchTimeout(0);

        eyes.setDebugScreenshotsPath("c:\\temp\\logs");
        eyes.setSaveDebugScreenshots(true);

        // This is your api key, make sure you use it in all your tests.
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        try {
            WebDriver eyesDriver = eyes.open(driver, "Wix", "Wix Example With Button");

            // Sign in to the page
            eyesDriver.get("https://eventstest.wixsite.com/events-page-e2e/events/ba837913-7dad-41b9-b530-6c2cbfc4c265");
            final String iFrameID = "TPAMultiSection_j5ocg4p8iframe";

            //Scroll to the bottom of the page
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("window.scrollBy(0,2000)", "");

            //Switch to frame with regular WD and return to default
            driver.switchTo().frame(iFrameID);
            driver.switchTo().defaultContent();

            //Switch to frame with Eyes WD and it will scroll to top of the frame
            eyesDriver.switchTo().frame(iFrameID);

            eyes.close();
        } finally {
            // Abort test in case of an unexpected error.
            eyes.abortIfNotClosed();
            driver.quit();
        }
    }
}