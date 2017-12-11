/*
 * Applitools SDK for Appium integration.
 */
package com.applitools.eyes.appium;

import com.applitools.eyes.AppEnvironment;
import com.applitools.eyes.ScaleProviderFactory;
import com.applitools.eyes.selenium.ContextBasedScaleProviderFactory;
import com.applitools.eyes.selenium.EyesSeleniumUtils;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;


public class Eyes extends com.applitools.eyes.selenium.Eyes {

    private static final String NATIVE_APP = "NATIVE_APP";
    protected EyesAppiumDriver driver;

    public Eyes() {
        init();
    }

    private void init() {
        EyesSeleniumUtils.setImageOrientationHandlerHandler(new AppiumImageOrientationHandler());
        EyesSeleniumUtils.setJavascriptHandler(new AppiumJavascriptHandler(this.driver));
    }

    protected EyesAppiumDriver getEyesDriver () { return this.driver; }

    @Override
    protected void initDriver(WebDriver driver) {
        if (driver instanceof AppiumDriver) {
            logger.verbose("Found an instance of AppiumDriver, so using EyesAppiumDriver instead");
            this.driver = new EyesAppiumDriver(logger, this, (AppiumDriver) driver);
        } else {
            logger.verbose("Did not find an instance of AppiumDriver, using regular logic");
            /* TODO
               when a breaking change of this library can be published, we can do away with
               this else clause */
            super.initDriver(driver);
        }
    }

    protected ScaleProviderFactory getScaleProviderFactory() {
        return new ContextBasedScaleProviderFactory(logger, positionProvider.getEntireSize(),
                viewportSizeHandler.get(), getDevicePixelRatio(), EyesAppiumUtils.isMobileDevice(getDriver()),
                scaleProviderHandler);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This override also checks for mobile operating system.
     */
    @Override
    protected AppEnvironment getAppEnvironment() {

        AppEnvironment appEnv = super.getAppEnvironment();
        RemoteWebDriver underlyingDriver = driver.getRemoteWebDriver();
        // If hostOs isn't set, we'll try and extract and OS ourselves.
        if (appEnv.getOs() == null) {
            logger.log("No OS set, checking for mobile OS...");
            if (EyesAppiumUtils.isMobileDevice(underlyingDriver)) {
                String platformName = null;
                logger.log("Mobile device detected! Checking device type..");
                if (EyesAppiumUtils.isAndroid(underlyingDriver)) {
                    logger.log("Android detected.");
                    platformName = "Android";
                } else if (EyesAppiumUtils.isIOS(underlyingDriver)) {
                    logger.log("iOS detected.");
                    platformName = "iOS";
                } else {
                    logger.log("Unknown device type.");
                }
                // We only set the OS if we identified the device type.
                if (platformName != null) {
                    String os = platformName;
                    String platformVersion = EyesAppiumUtils.getPlatformVersion(underlyingDriver);
                    if (platformVersion != null) {
                        String majorVersion =
                                platformVersion.split("\\.", 2)[0];

                        if (!majorVersion.isEmpty()) {
                            os += " " + majorVersion;
                        }
                    }

                    logger.verbose("Setting OS: " + os);
                    appEnv.setOs(os);
                }
            } else {
                logger.log("No mobile OS detected.");
            }
        }
        logger.log("Done!");
        return appEnv;
    }
}