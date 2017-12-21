package com.applitools.eyes.selenium;

import com.applitools.eyes.FixedCutProvider;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.Statement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.List;

@RunWith(JUnit4.class)
public class WizzAirTest extends TestSetup {
    @ClassRule
    public static final TestRule setTestSuitName = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            testSuitName = "WizzAirTest";
            testedPageUrl = "https://wizzair.com/en-gb/main-page/#/booking/select-flight/BUD/EIN/2017-12-11/2018-01-21/1/0/0";
            testedPageSize = new RectangleSize(1266,800);
            hideScrollbars = false;
            forceFullPageScreenshot = false;
        }
    };

    @Rule
    public final TestRule beforeTest = new TestWatcher() {
        @Override
        public Statement apply(Statement statement, Description description) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("disable-infobars");
            //options.addArguments("headless");

            //Run locally
            //-----------
            //webDriver = new ChromeDriver(options);


            //Run Remotely
            //------------
            caps = DesiredCapabilities.chrome();
            caps.setCapability(ChromeOptions.CAPABILITY, options);

            return statement;
        }
    };

    @Test
    public void TestWizzAir() {
        List<WebElement> cookieMessage = driver.findElements(By.cssSelector(".cookie-policy__button"));
        if (cookieMessage.size() > 0) {
            cookieMessage.toArray(new WebElement[0])[0].click();
        }
        ((EyesWebDriver)driver).executeScript("document.querySelector(\".body--booking-flow\").style.overflowY=\"scroll\"");
        eyes.setImageCut(new FixedCutProvider(0,0,0,17));
        eyes.check("Search", Target.window().fully());
    }

}
