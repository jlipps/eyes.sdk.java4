package com.applitools.eyes.selenium;

import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.fluent.Target;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class IgnoreRegionThroughCode {

    public static void main(String[] args) throws InterruptedException {

        // Open a Chrome browser.
        ChromeOptions options = new ChromeOptions();
        options.addArguments("disable-infobars");
        //options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);

        // Initialize the eyes SDK and set your private API key.
        Eyes eyes = new Eyes();
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        eyes.setForceFullPageScreenshot(true);
        eyes.setStitchMode(StitchMode.CSS);

        try{

            eyes.open(driver, "Applitools", "Apply Ignore region through code", new RectangleSize(1200, 600));

            driver.get("https://applitools.com/");
            WebElement DivElement = driver.findElement(By.cssSelector("body > div.page.unpadded > div.homepage > div:nth-child(3) > div > div > div"));
            WebElement IgnoreElement = driver.findElement(By.cssSelector("body > div.page.unpadded > div.homepage > div:nth-child(3) > div > div > div > div:nth-child(1) > div"));
//            eyes.check("StepName",Target.region(DivElement).ignore(IgnoreElement));
            eyes.check("StepName",Target.region(DivElement).floating(IgnoreElement,10,10,10,10));

            eyes.close();


        } finally {

            // Close the browser.
            driver.quit();

            // If the test was aborted before eyes.close was called, ends the test as aborted.
            eyes.abortIfNotClosed();
        }

    }

}