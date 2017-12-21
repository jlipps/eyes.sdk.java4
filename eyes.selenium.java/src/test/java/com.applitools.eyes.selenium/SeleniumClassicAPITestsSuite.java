package com.applitools.eyes.selenium;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestClassicApi_Chrome.class,
        TestClassicApi_Chrome_ForceFullPageScreenShot.class,
        TestClassicApi_Firefox.class,
        TestClassicApi_Firefox_ForceFullPageScreenshot.class
})

public class SeleniumClassicAPITestsSuite {
}
