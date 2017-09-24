package com.applitools.eyes.selenium;

import com.applitools.eyes.LogHandler;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.selenium.fluent.Target;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.net.URISyntaxException;

public class TestWix {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        // Test 1
        WebDriver driver = new ChromeDriver();

        Eyes eyes = new Eyes();

        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));

        eyes.setForceFullPageScreenshot(true);
        eyes.setStitchMode(StitchMode.CSS);

        LogHandler logHandler;
        //logHandler = new FileLogger("c:\\temp\\logs\\Java\\TestElement.log", true, true);
        logHandler = new StdoutLogHandler(true);
        eyes.setLogHandler(logHandler);

        eyes.setDebugScreenshotsPath("c:\\temp\\logs");
        eyes.setSaveDebugScreenshots(true);

        try {
            driver = eyes.open(driver, "Eyes Selenium SDK", "Wix frame test",
                    new RectangleSize(1024, 600));

            driver.get("https://eventstest.wixsite.com/events-page-e2e/events/ba837913-7dad-41b9-b530-6c2cbfc4c265");

            eyes.check("map", Target.frame(By.id("TPAMultiSection_j5ocg4p8iframe")).region(By.id("map")));
//            eyes.checkRegionInFrame("TPAMultiSection_j5ocg4p8iframe", By.id("map"));

            eyes.close();

        } finally {
            eyes.abortIfNotClosed();
            driver.quit();
        }
    }
}
