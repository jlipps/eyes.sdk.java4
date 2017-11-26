package com.applitools.eyes.selenium;

import com.applitools.eyes.selenium.fluent.Target;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import com.applitools.eyes.RectangleSize;


public class AdidasTest {

    public static void main(String[] args) throws InterruptedException {

        // Open a Chrome browser.
        WebDriver driver = new ChromeDriver();

        // Initialize the eyes SDK and set your private API key.
        Eyes eyes = new Eyes();
        eyes.setApiKey(System.getenv("Applitools_ApiKey"));
        eyes.setForceFullPageScreenshot(true);
        eyes.setStitchMode(StitchMode.CSS);

        eyes.setSaveDebugScreenshots(true);
        eyes.setDebugScreenshotsPath("c:\\temp\\logs");

        try {
            eyes.open(driver, "Adidas Region", "Adidas Region", new RectangleSize(1380, 680));
            Thread.sleep(3000);
            driver.get("http://www.adidas.com/us/soccer-shoes");

            eyes.setDebugScreenshotsPrefix("adidas_region_fluent_");
            eyes.check("Work As expected", Target.region(By.cssSelector("#product-grid")));

            eyes.setDebugScreenshotsPrefix("adidas_region_classic_");
            eyes.checkRegion(By.cssSelector("#product-grid"),"Not working as expected");
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