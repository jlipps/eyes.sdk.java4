/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes.selenium;

import com.applitools.eyes.CoordinatesType;
import com.applitools.eyes.CutProvider;
import com.applitools.eyes.EyesBase;
import com.applitools.eyes.EyesException;
import com.applitools.eyes.EyesScreenshot;
import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.MatchWindowDataWithScreenshot;
import com.applitools.eyes.NullCutProvider;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.Region;
import com.applitools.eyes.ScaleProvider;
import com.applitools.eyes.ScaleProviderFactory;
import com.applitools.eyes.ScaleProviderIdentityFactory;
import com.applitools.eyes.SessionType;
import com.applitools.eyes.UserAgent;
import com.applitools.eyes.capture.EyesScreenshotFactory;
import com.applitools.eyes.capture.ImageProvider;
import com.applitools.eyes.diagnostics.TimedAppOutput;
import com.applitools.eyes.exceptions.TestFailedException;
import com.applitools.eyes.fluent.ICheckSettings;
import com.applitools.eyes.fluent.ICheckSettingsInternal;
import com.applitools.eyes.positioning.NullRegionProvider;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.positioning.RegionProvider;
import com.applitools.eyes.positioning.ScrollingPositionProvider;
import com.applitools.eyes.scaling.FixedScaleProviderFactory;
import com.applitools.eyes.scaling.NullScaleProvider;
import com.applitools.eyes.selenium.capture.EyesWebDriverScreenshot;
import com.applitools.eyes.selenium.capture.EyesWebDriverScreenshotFactory;
import com.applitools.eyes.selenium.capture.FullPageCaptureAlgorithm;
import com.applitools.eyes.selenium.capture.ImageProviderFactory;
import com.applitools.eyes.selenium.exceptions.EyesDriverOperationException;
import com.applitools.eyes.selenium.fluent.FrameLocator;
import com.applitools.eyes.selenium.fluent.ISeleniumCheckTarget;
import com.applitools.eyes.selenium.fluent.ISeleniumFrameCheckTarget;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.selenium.frames.Frame;
import com.applitools.eyes.selenium.frames.FrameChain;
import com.applitools.eyes.selenium.positioning.CssTranslatePositionProvider;
import com.applitools.eyes.selenium.positioning.ElementPositionProvider;
import com.applitools.eyes.selenium.positioning.ImageRotation;
import com.applitools.eyes.selenium.positioning.RegionPositionCompensation;
import com.applitools.eyes.selenium.positioning.RegionPositionCompensationFactory;
import com.applitools.eyes.selenium.positioning.ScrollPositionProvider;
import com.applitools.eyes.selenium.positioning.SeleniumScrollingPositionProvider;
import com.applitools.eyes.selenium.regionVisibility.MoveToRegionVisibilityStrategy;
import com.applitools.eyes.selenium.regionVisibility.NopRegionVisibilityStrategy;
import com.applitools.eyes.selenium.regionVisibility.RegionVisibilityStrategy;
import com.applitools.eyes.selenium.wrappers.EyesRemoteWebElement;
import com.applitools.eyes.selenium.wrappers.EyesTargetLocator;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import com.applitools.eyes.triggers.MouseAction;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.ImageUtils;
import com.applitools.utils.PropertyHandler;
import com.applitools.utils.ReadOnlyPropertyHandler;
import com.applitools.utils.SimplePropertyHandler;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * The main API gateway for the SDK.
 */
public class Eyes extends EyesBase {

    public interface WebDriverAction {
        void drive(WebDriver driver);
    }

    public static final double UNKNOWN_DEVICE_PIXEL_RATIO = 0;
    public static final double DEFAULT_DEVICE_PIXEL_RATIO = 1;

    private static final int USE_DEFAULT_MATCH_TIMEOUT = -1;

    // Seconds
    private static final int RESPONSE_TIME_DEFAULT_DEADLINE = 10;
    // Seconds
    private static final int RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE = 20;

    // Milliseconds
    private static final int DEFAULT_WAIT_BEFORE_SCREENSHOTS = 100;

    protected EyesWebDriver driver;
    private boolean dontGetTitle;

    private boolean forceFullPageScreenshot;
    private boolean checkFrameOrElement;

    private String originalDefaultContentOverflow;
    private String originalFrameOverflow;

    public Region getRegionToCheck() {
        return regionToCheck;
    }

    public void setRegionToCheck(Region regionToCheck) {
        this.regionToCheck = regionToCheck;
    }

    private Region regionToCheck = null;

    private boolean hideScrollbars;
    private ImageRotation rotation;
    protected double devicePixelRatio;
    private StitchMode stitchMode;
    private int waitBeforeScreenshots;
    private RegionVisibilityStrategy regionVisibilityStrategy;
    private ElementPositionProvider elementPositionProvider;
    private SeleniumJavaScriptExecutor jsExecutor;

    private UserAgent userAgent;
    protected ImageProvider imageProvider;
    protected RegionPositionCompensation regionPositionCompensation;
    private WebElement targetElement = null;

    private boolean stitchContent = false;

    public boolean shouldStitchContent() {
        return stitchContent;
    }

    /**
     * Creates a new (possibly disabled) Eyes instance that interacts with the
     * Eyes Server at the specified url.
     * @param serverUrl The Eyes server URL.
     */
    public Eyes(URI serverUrl) {
        super(serverUrl);

        checkFrameOrElement = false;
        forceFullPageScreenshot = false;
        dontGetTitle = false;
        hideScrollbars = false;
        devicePixelRatio = UNKNOWN_DEVICE_PIXEL_RATIO;
        stitchMode = StitchMode.SCROLL;
        waitBeforeScreenshots = DEFAULT_WAIT_BEFORE_SCREENSHOTS;
        regionVisibilityStrategy = new MoveToRegionVisibilityStrategy(logger);
    }

    /**
     * Creates a new Eyes instance that interacts with the Eyes cloud
     * service.
     */
    public Eyes() {
        this(getDefaultServerUrl());
    }

    @Override
    public String getBaseAgentId() {
        return "eyes.selenium.java/4.0";
    }

    public WebDriver getDriver() {
        return driver;
    }
    protected EyesWebDriver getEyesDriver () { return this.driver; }

    /**
     * ﻿Forces a full page screenshot (by scrolling and stitching) if the
     * browser only ﻿supports viewport screenshots).
     * @param shouldForce Whether to force a full page screenshot or not.
     */
    public void setForceFullPageScreenshot(boolean shouldForce) {
        forceFullPageScreenshot = shouldForce;
    }

    /**
     * @return Whether Eyes should force a full page screenshot.
     */
    public boolean getForceFullPageScreenshot() {
        return forceFullPageScreenshot;
    }

    /**
     * Sets the time to wait just before taking a screenshot (e.g., to allow
     * positioning to stabilize when performing a full page stitching).
     * @param waitBeforeScreenshots The time to wait (Milliseconds). Values
     *                              smaller or equal to 0, will cause the
     *                              default value to be used.
     */
    public void setWaitBeforeScreenshots(int waitBeforeScreenshots) {
        if (waitBeforeScreenshots <= 0) {
            this.waitBeforeScreenshots = DEFAULT_WAIT_BEFORE_SCREENSHOTS;
        } else {
            this.waitBeforeScreenshots = waitBeforeScreenshots;
        }
    }

    /**
     * @return The time to wait just before taking a screenshot.
     */
    public int getWaitBeforeScreenshots() {
        return waitBeforeScreenshots;
    }

    /**
     * Turns on/off the automatic scrolling to a region being checked by
     * {@code checkRegion}.
     * @param shouldScroll Whether to automatically scroll to a region being validated.
     */
    public void setScrollToRegion(boolean shouldScroll) {
        if (shouldScroll) {
            regionVisibilityStrategy = new MoveToRegionVisibilityStrategy(logger);
        } else {
            regionVisibilityStrategy = new NopRegionVisibilityStrategy(logger);
        }
    }

    /**
     * @return Whether to automatically scroll to a region being validated.
     */
    public boolean getScrollToRegion() {
        return !(regionVisibilityStrategy instanceof NopRegionVisibilityStrategy);
    }

    /**
     * Set the type of stitching used for full page screenshots. When the
     * page includes fixed position header/sidebar, use {@link StitchMode#CSS}.
     * Default is {@link StitchMode#SCROLL}.
     * @param mode The stitch mode to set.
     */
    public void setStitchMode(StitchMode mode) {
        logger.verbose("setting stitch mode to " + mode);
        stitchMode = mode;
        if (driver != null) {
            initPositionProvider();
        }
    }

    /**
     * @return The current stitch mode settings.
     */
    public StitchMode getStitchMode() {
        return stitchMode;
    }

    /**
     * Hide the scrollbars when taking screenshots.
     * @param shouldHide Whether to hide the scrollbars or not.
     */
    public void setHideScrollbars(boolean shouldHide) {
        hideScrollbars = shouldHide;
    }

    /**
     * @return Whether or not scrollbars are hidden when taking screenshots.
     */
    public boolean getHideScrollbars() {
        return hideScrollbars;
    }

    /**
     * @return The image rotation data.
     */
    public ImageRotation getRotation() {
        return rotation;
    }

    /**
     * @param rotation The image rotation data.
     */
    public void setRotation(ImageRotation rotation) {
        this.rotation = rotation;
        if (driver != null) {
            getEyesDriver().setRotation(rotation);
        }
    }

    /**
     * @return The device pixel ratio, or {@link #UNKNOWN_DEVICE_PIXEL_RATIO}
     * if the DPR is not known yet or if it wasn't possible to extract it.
     */
    public double getDevicePixelRatio() {
        return devicePixelRatio;
    }

    /**
     * See {@link #open(WebDriver, String, String, RectangleSize, SessionType)}.
     * {@code sessionType} defaults to {@code null}.
     */
    public WebDriver open(WebDriver driver, String appName, String testName,
                          RectangleSize viewportSize) {
        return open(driver, appName, testName, viewportSize, null);
    }

    /**
     * See {@link #open(WebDriver, String, String, SessionType)}.
     * {@code viewportSize} defaults to {@code null}.
     * {@code sessionType} defaults to {@code null}.
     */
    public WebDriver open(WebDriver driver, String appName, String testName) {
        return open(driver, appName, testName, null, null);
    }


    /**
     * Starts a test.
     * @param driver       The web driver that controls the browser hosting
     *                     the application under test.
     * @param appName      The name of the application under test.
     * @param testName     The test name.
     * @param viewportSize The required browser's viewport size
     *                     (i.e., the visible part of the document's body) or
     *                     {@code null} to use the current window's viewport.
     * @param sessionType  The type of test (e.g.,  standard test / visual
     *                     performance test).
     * @return A wrapped WebDriver which enables Eyes trigger recording and
     * frame handling.
     */
    protected WebDriver open(WebDriver driver, String appName, String testName,
                             RectangleSize viewportSize, SessionType sessionType) {

        if (getIsDisabled()) {
            logger.verbose("Ignored");
            return driver;
        }

        initDriver(driver);

        logger.verbose("!!!! about to get user agent");
        String uaString = getEyesDriver().getUserAgent();
        if (uaString != null) {
            userAgent = UserAgent.ParseUserAgentString(uaString, true);
        }

        imageProvider = ImageProviderFactory.getImageProvider(userAgent, this, logger, getEyesDriver());
        regionPositionCompensation = RegionPositionCompensationFactory.getRegionPositionCompensation(userAgent, this, logger);

        openBase(appName, testName, viewportSize, sessionType);
        ArgumentGuard.notNull(driver, "driver");

        devicePixelRatio = UNKNOWN_DEVICE_PIXEL_RATIO;
        this.jsExecutor = new SeleniumJavaScriptExecutor(getEyesDriver());
        initPositionProvider();

        getEyesDriver().setRotation(rotation);
        return getEyesDriver();
    }

    protected void initDriver(WebDriver driver) {
        if (driver instanceof RemoteWebDriver) {
            this.driver = new EyesWebDriver(logger, this, (RemoteWebDriver) driver);
        } else if (driver instanceof EyesWebDriver) {
            this.driver = (EyesWebDriver) driver;
        } else {
            String errMsg = "Driver is not a RemoteWebDriver (" +
                    driver.getClass().getName() + ")";
            logger.log(errMsg);
            throw new EyesException(errMsg);
        }
    }

    private void initPositionProvider() {
        // Setting the correct position provider.
        StitchMode stitchMode = getStitchMode();
        logger.verbose("initializing position provider. stitchMode: " + stitchMode);
        switch (stitchMode) {
            case CSS:
                setPositionProvider(new CssTranslatePositionProvider(logger, this.jsExecutor));
                break;
            default:
                setPositionProvider(getScrollPositionProvider());
        }
    }

    @Override
    public void setPositionProvider(PositionProvider positionProvider) {
        this.positionProvider = positionProvider;
    }

    /**
     * See {@link #open(WebDriver, String, String, RectangleSize)}.
     * {@code viewportSize} defaults to {@code null}.
     */
    protected WebDriver open(WebDriver driver, String appName, String testName, SessionType sessionType) {
        return open(driver, appName, testName, null, sessionType);
    }

    /**
     * See {@link #checkWindow(String)}.
     * {@code tag} defaults to {@code null}.
     * Default match timeout is used.
     */
    public void checkWindow() {
        checkWindow(null);
    }

    /**
     * See {@link #checkWindow(int, String)}.
     * Default match timeout is used.
     * @param tag An optional tag to be associated with the snapshot.
     */
    public void checkWindow(String tag) {
        checkWindow(USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Takes a snapshot of the application under test and matches it with
     * the expected output.
     * @param matchTimeout The amount of time to retry matching (Milliseconds).
     * @param tag          An optional tag to be associated with the snapshot.
     * @throws TestFailedException Thrown if a mismatch is detected and
     *                             immediate failure reports are enabled.
     */
    public void checkWindow(int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckWindow(%d, '%s'): Ignored", matchTimeout, tag));
            return;
        }

        logger.log(String.format("CheckWindow(%d, '%s')", matchTimeout, tag));

        this.regionToCheck = null;

        super.checkWindowBase(
                NullRegionProvider.INSTANCE,
                tag,
                false,
                matchTimeout
        );
    }

    /**
     * Runs a test on the current window.
     * @param driver       The web driver that controls the browser hosting
     *                     the application under test.
     * @param appName      The name of the application under test.
     * @param testName     The test name (will also be used as the tag name for the step).
     * @param viewportSize The required browser's viewport size
     *                     (i.e., the visible part of the document's body) or
     *                     {@code null} to use the current window's viewport.
     */
    public void testWindow(WebDriver driver, String appName, String testName,
                           RectangleSize viewportSize) {
        open(driver, appName, testName, viewportSize);
        try {
            checkWindow(testName);
            close();
        } finally {
            abortIfNotClosed();
        }
    }

    /**
     * See {@link #testWindow(WebDriver, String, String, RectangleSize)}.
     * {@code viewportSize} defaults to {@code null}.
     */
    public void testWindow(WebDriver driver, String appName, String testName) {
        testWindow(driver, appName, testName, null);
    }

    /**
     * See {@link #testWindow(WebDriver, String, String, RectangleSize)}.
     * {@code appName} defaults to {@code null} (which means the name set in
     * {@link #setAppName(String)} would be used.
     */
    public void testWindow(WebDriver driver, String testName,
                           RectangleSize viewportSize) {
        testWindow(driver, null, testName, viewportSize);
    }

    /**
     * See {@link #testWindow(WebDriver, String, RectangleSize)}.
     * {@code viewportSize} defaults to {@code null}.
     */
    public void testWindow(WebDriver driver, String testName) {
        testWindow(driver, testName, (RectangleSize) null);
    }

    /**
     * Run a visual performance test.
     * @param driver   The driver to use.
     * @param appName  The name of the application being tested.
     * @param testName The test name.
     * @param action   Actions to be performed in parallel to starting the test.
     * @param deadline The expected time until the application should have been loaded. (Seconds)
     * @param timeout  The maximum time until the application should have been loaded. (Seconds)
     */
    public void testResponseTime(final WebDriver driver, String appName,
                                 String testName, final WebDriverAction action,
                                 int deadline, int timeout) {
        open(driver, appName, testName, SessionType.PROGRESSION);
        Runnable runnableAction = null;
        if (action != null) {
            runnableAction = new Runnable() {
                public void run() {
                    action.drive(driver);
                }
            };
        }

        MatchWindowDataWithScreenshot result =
                super.testResponseTimeBase(NullRegionProvider.INSTANCE,
                        runnableAction,
                        deadline,
                        timeout,
                        5000);

        logger.verbose("Checking if deadline was exceeded...");
        boolean deadlineExceeded = true;
        if (result != null) {
            TimedAppOutput tao =
                    (TimedAppOutput) result.getMatchWindowData().getAppOutput();
            long resultElapsed = tao.getElapsed();
            long deadlineMs = deadline * 1000;
            logger.verbose(String.format(
                    "Deadline: %d, Elapsed time for match: %d",
                    deadlineMs, resultElapsed));
            deadlineExceeded = resultElapsed > deadlineMs;
        }
        logger.verbose("Deadline exceeded? " + deadlineExceeded);

        closeResponseTime(deadlineExceeded);
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int)}.
     * {@code timeout} defaults to {@code deadline} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, WebDriverAction action,
                                 int deadline) {
        testResponseTime(driver, appName, testName, action, deadline,
                (deadline + RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE));
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int)}.
     * {@code deadline} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE}.
     * {@code timeout} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, WebDriverAction action) {
        testResponseTime(driver, appName, testName, action,
                RESPONSE_TIME_DEFAULT_DEADLINE,
                (RESPONSE_TIME_DEFAULT_DEADLINE +
                        RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE));
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int)}.
     * {@code action} defaults to {@code null}.
     * {@code timeout} defaults to {@code deadline} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, int deadline) {
        testResponseTime(driver, appName, testName, null, deadline,
                (deadline + RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE));
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int)}.
     * {@code deadline} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE}.
     * {@code timeout} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     * {@code action} defaults to {@code null}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName) {
        testResponseTime(driver, appName, testName, null,
                RESPONSE_TIME_DEFAULT_DEADLINE,
                (RESPONSE_TIME_DEFAULT_DEADLINE +
                        RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE));
    }

    /**
     * Similar to {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int)},
     * except this method sets the viewport size before starting the
     * performance test.
     * @param viewportSize The required viewport size.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, WebDriverAction action,
                                 int deadline, int timeout,
                                 RectangleSize viewportSize) {
        // Notice we specifically use the setViewportSize overload which does
        // not handle frames (as we want to make sure this is as fast as
        // possible).
        setViewportSize(driver, viewportSize);

        testResponseTime(driver, appName, testName, action, deadline, timeout);
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int, RectangleSize)}.
     * {@code timeout} defaults to {@code deadline} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, WebDriverAction action,
                                 int deadline, RectangleSize viewportSize) {
        testResponseTime(driver, appName, testName, action, deadline,
                (deadline + RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE),
                viewportSize);
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int, RectangleSize)}.
     * {@code deadline} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE}.
     * {@code timeout} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, WebDriverAction action,
                                 RectangleSize viewportSize) {
        testResponseTime(driver, appName, testName, action,
                RESPONSE_TIME_DEFAULT_DEADLINE,
                (RESPONSE_TIME_DEFAULT_DEADLINE +
                        RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE),
                viewportSize);
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, WebDriverAction, int, int, RectangleSize)}.
     * {@code action} defaults to {@code null}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, int deadline, int timeout,
                                 RectangleSize viewportSize) {
        testResponseTime(driver, appName, testName, null, deadline, timeout,
                viewportSize);
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, int, int, RectangleSize)}.
     * {@code timeout} defaults to {@code deadline} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, int deadline,
                                 RectangleSize viewportSize) {
        testResponseTime(driver, appName, testName, deadline,
                (deadline + RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE),
                viewportSize);
    }

    /**
     * See {@link #testResponseTime(WebDriver, String, String, int, int, RectangleSize)}.
     * {@code deadline} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE}.
     * {@code timeout} defaults to {@link #RESPONSE_TIME_DEFAULT_DEADLINE} + {@link #RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE}.
     */
    public void testResponseTime(WebDriver driver, String appName,
                                 String testName, RectangleSize viewportSize) {
        testResponseTime(driver, appName, testName,
                RESPONSE_TIME_DEFAULT_DEADLINE,
                (RESPONSE_TIME_DEFAULT_DEADLINE +
                        RESPONSE_TIME_DEFAULT_DIFF_FROM_DEADLINE),
                viewportSize);
    }

    public void check(String name, ICheckSettings checkSettings) {
        ArgumentGuard.notNull(checkSettings, "checkSettings");

        logger.verbose(String.format("check(\"%s\", checkSettings) - begin", name));

        ICheckSettingsInternal checkSettingsInternal = (ICheckSettingsInternal) checkSettings;
        ISeleniumCheckTarget seleniumCheckTarget = (checkSettings instanceof ISeleniumCheckTarget) ? (ISeleniumCheckTarget) checkSettings : null;

        this.stitchContent = checkSettingsInternal.getStitchContent();

        final Region targetRegion = checkSettingsInternal.getTargetRegion();

        int switchedToFrameCount = this.switchToFrame(seleniumCheckTarget);

        this.regionToCheck = null;

        if (targetRegion != null) {
            this.checkWindowBase(new RegionProvider() {
                @Override
                public Region getRegion() {
                    return targetRegion;
                }
            }, name, false, checkSettings);
        } else if (seleniumCheckTarget != null) {
            By targetSelector = seleniumCheckTarget.getTargetSelector();
            WebElement targetElement = seleniumCheckTarget.getTargetElement();
            if (targetElement == null && targetSelector != null) {
                targetElement = getEyesDriver().findElement(targetSelector);
            }
            if (targetElement != null) {
                this.targetElement = targetElement;
                if (this.stitchContent) {
                    this.checkElement(name, checkSettings);
                } else {
                    this.checkRegion(name, checkSettings);
                }
                this.targetElement = null;
            } else if (seleniumCheckTarget.getFrameChain().size() > 0) {
                if (this.stitchContent) {
                    this.checkFullFrameOrElement(name, checkSettings);
                } else {
                    this.checkFrameFluent(name, checkSettings);
                }
            } else {
                this.checkWindowBase(NullRegionProvider.INSTANCE, name, false, checkSettings);
            }
        }

        while (switchedToFrameCount > 0) {
            getEyesDriver().switchTo().parentFrame();
            switchedToFrameCount--;
        }

        this.stitchContent = false;

        logger.verbose("check - done!");
    }

    protected void checkFrameFluent(String name, ICheckSettings checkSettings) {
        FrameChain frameChain = new FrameChain(logger, getEyesDriver().getFrameChain());
        Frame targetFrame = frameChain.pop();
        this.targetElement = targetFrame.getReference();

        EyesTargetLocator switchTo = (EyesTargetLocator) getEyesDriver().switchTo();
        switchTo.framesDoScroll(frameChain);

        this.checkRegion(name, checkSettings);

        this.targetElement = null;
    }

    private int switchToFrame(ISeleniumCheckTarget checkTarget) {
        if (checkTarget == null) {
            return 0;
        }

        List<FrameLocator> frameChain = checkTarget.getFrameChain();
        int switchedToFrameCount = 0;
        for (FrameLocator frameLocator : frameChain) {
            if (switchToFrame(frameLocator)) {
                switchedToFrameCount++;
            }
        }
        return switchedToFrameCount;
    }

    private boolean switchToFrame(ISeleniumFrameCheckTarget frameTarget) {
        WebDriver.TargetLocator switchTo = getEyesDriver().switchTo();

        if (frameTarget.getFrameIndex() != null) {
            switchTo.frame(frameTarget.getFrameIndex());
            return true;
        }

        if (frameTarget.getFrameNameOrId() != null) {
            switchTo.frame(frameTarget.getFrameNameOrId());
            return true;
        }

        if (frameTarget.getFrameSelector() != null) {
            WebElement frameElement = getEyesDriver().findElement(frameTarget.getFrameSelector());
            if (frameElement != null) {
                switchTo.frame(frameElement);
                return true;
            }
        }

        return false;
    }

    private void checkFullFrameOrElement(String name, ICheckSettings checkSettings) {
        checkFrameOrElement = true;

        logger.verbose("checkFullFrameOrElement()");

        checkWindowBase(new RegionProvider() {
            @Override
            public Region getRegion() {
                if (checkFrameOrElement) {

                    FrameChain fc = ensureFrameVisible();

                    // FIXME - Scaling should be handled in a single place instead
                    ScaleProviderFactory scaleProviderFactory = updateScalingParams();

                    BufferedImage screenshotImage = imageProvider.getImage();

                    debugScreenshotsProvider.save(screenshotImage, "checkFullFrameOrElement");

                    scaleProviderFactory.getScaleProvider(screenshotImage.getWidth());

                    EyesTargetLocator switchTo = (EyesTargetLocator) getEyesDriver().switchTo();
                    switchTo.frames(fc);

                    final EyesWebDriverScreenshot screenshot = new EyesWebDriverScreenshot(logger, getEyesDriver(), screenshotImage);

                    logger.verbose("replacing regionToCheck");
                    setRegionToCheck(screenshot.getFrameWindow());
                }

                return Region.EMPTY;
            }
        }, name, false, checkSettings);

        checkFrameOrElement = false;
    }

    private void setPositionByElement(WebElement element) {
        PositionProvider pp = getPositionProvider();
        // if we know how to set the position by the element, do that (this includes appium)
        if (pp instanceof SeleniumScrollingPositionProvider) {
            ((SeleniumScrollingPositionProvider) pp).setPosition(element);
        } else {
            // otherwise, set by the location (appium is not so good at this)
            Point p = element.getLocation();
            Location elementLocation = new Location(p.getX(), p.getY());
            pp.setPosition(elementLocation);
        }
    }

    private FrameChain ensureFrameVisible() {
        FrameChain originalFC = new FrameChain(logger, getEyesDriver().getFrameChain());
        FrameChain fc = new FrameChain(logger, getEyesDriver().getFrameChain());
        PositionProvider pp = getPositionProvider();
        while (fc.size() > 0) {
            getEyesDriver().getRemoteWebDriver().switchTo().parentFrame();
            Frame frame = fc.pop();
            pp.setPosition(frame.getLocation());
        }
        ((EyesTargetLocator) getEyesDriver().switchTo()).frames(originalFC);
        return originalFC;
    }

    private void ensureElementVisible(WebElement element) {
        if (this.targetElement == null) {
            // No element? we must be checking the window.
            return;
        }

        FrameChain originalFC = new FrameChain(logger, getEyesDriver().getFrameChain());
        EyesTargetLocator switchTo = (EyesTargetLocator) getEyesDriver().switchTo();

        EyesRemoteWebElement eyesRemoteWebElement = getEyesDriver().getEyesElement(element);
        Region elementBounds = eyesRemoteWebElement.getBounds();

        Location currentFrameOffset = originalFC.getCurrentFrameOffset();
        elementBounds = elementBounds.offset(currentFrameOffset.getX(), currentFrameOffset.getY());

        Region viewportBounds = getViewportScrollBounds();

        if (!viewportBounds.contains(elementBounds)) {
            ensureFrameVisible();

            if (originalFC.size() > 0 && !element.equals(originalFC.peek())) {
                switchTo.frames(originalFC);
            }

            setPositionByElement(element);
        }
    }

    private Region getViewportScrollBounds() {
        FrameChain originalFrameChain = new FrameChain(logger, getEyesDriver().getFrameChain());
        EyesTargetLocator switchTo = (EyesTargetLocator) getEyesDriver().switchTo();
        switchTo.defaultContent();
        ScrollingPositionProvider spp = getScrollPositionProvider();
        Location location = spp.getCurrentPosition();
        Region viewportBounds = new Region(location, getViewportSize());
        switchTo.frames(originalFrameChain);
        return viewportBounds;
    }

    private void checkRegion(String name, ICheckSettings checkSettings) {
        checkWindowBase(new RegionProvider() {
            @Override
            public Region getRegion() {
                Point p = targetElement.getLocation();
                Dimension d = targetElement.getSize();
                return new Region(p.getX(), p.getY(), d.getWidth(), d.getHeight(), CoordinatesType.CONTEXT_RELATIVE);
            }
        }, name, false, checkSettings);
        logger.verbose("Done! trying to scroll back to original position..");
    }

    /**
     * See {@link #checkRegion(Region, int, String)}.
     * {@code tag} defaults to {@code null}.
     * Default match timeout is used.
     */
    public void checkRegion(Region region) {
        checkRegion(region, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * Takes a snapshot of the application under test and matches a specific region within it with the expected output.
     * @param region       A non empty region representing the screen region to check.
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     * @throws TestFailedException Thrown if a mismatch is detected and immediate failure reports are enabled.
     */
    public void checkRegion(final Region region, int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckRegion([%s], %d, '%s'): Ignored", region, matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(region, "region");

        logger.verbose(String.format("CheckRegion([%s], %d, '%s')", region, matchTimeout, tag));

        super.checkWindowBase(
                new RegionProvider() {
                    public Region getRegion() {
                        return region;
                    }
                },
                tag,
                false,
                matchTimeout
        );
    }

    /**
     * See {@link #checkRegion(WebElement, String)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegion(WebElement element) {
        checkRegion(element, null);
    }

    /**
     * If {@code stitchContent} is {@code false} then behaves the same as
     * {@link #checkRegion(org.openqa.selenium.WebElement)}, otherwise
     * behaves the same as {@link #checkElement(WebElement)}.
     */
    public void checkRegion(WebElement element, boolean stitchContent) {
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(element);
        } else {
            checkRegion(element);
        }
        this.stitchContent = false;
    }

    /**
     * See {@link #checkRegion(WebElement, int, String)}.
     * Default match timeout is used.
     */
    public void checkRegion(WebElement element, String tag) {
        checkRegion(element, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * if {@code stitchContent} is {@code false} then behaves the same {@link
     * #checkRegion(org.openqa.selenium.WebElement, String)}. Otherwise
     * behaves the same as {@link #checkElement(WebElement, String)}.
     */
    public void checkRegion(WebElement element, String tag, boolean stitchContent) {
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(element, tag);
        } else {
            checkRegion(element, tag);
        }
        this.stitchContent = false;
    }

    /**
     * Takes a snapshot of the application under test and matches a region of
     * a specific element with the expected region output.
     * @param element      The element which represents the region to check.
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     * @throws TestFailedException if a mismatch is detected and
     *                             immediate failure reports are enabled
     */
    public void checkRegion(final WebElement element, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("CheckRegion(element, %d, '%s'): Ignored", matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(element, "element");

        logger.log(String.format("CheckRegion(element, %d, '%s')",
                matchTimeout, tag));

        this.regionToCheck = null;

        // If needed, scroll to the top/left of the element (additional help
        // to make sure it's visible).
        Point locationAsPoint = element.getLocation();
        regionVisibilityStrategy.moveToRegion(getPositionProvider(),
                new Location(locationAsPoint.getX(), locationAsPoint.getY()));

        super.checkWindowBase(
                new RegionProvider() {
                    public Region getRegion() {
                        Point p = element.getLocation();
                        Dimension d = element.getSize();
                        return new Region(p.getX(), p.getY(), d.getWidth(),
                                d.getHeight(), CoordinatesType.CONTEXT_RELATIVE);
                    }
                },
                tag,
                false,
                matchTimeout
        );
        logger.verbose("Done! trying to scroll back to original position..");
        regionVisibilityStrategy.returnToOriginalPosition(getPositionProvider());
        logger.verbose("Done!");
    }

    /**
     * if {@code stitchContent} is {@code false} then behaves the same {@link
     * #checkRegion(org.openqa.selenium.WebElement, int, String)}. Otherwise
     * behaves the same as {@link #checkElement(WebElement, String)}.
     */
    public void checkRegion(WebElement element, int matchTimeout, String tag, boolean stitchContent) {
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(element, matchTimeout, tag);
        } else {
            checkRegion(element, matchTimeout, tag);
        }
        this.stitchContent = false;
    }

    /**
     * See {@link #checkRegion(By, String)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegion(By selector) {
        checkRegion(selector, null);
    }

    /**
     * If {@code stitchContent} is {@code false} then behaves the same as
     * {@link #checkRegion(org.openqa.selenium.By)}. Otherwise, behaves the
     * same as {@code #checkElement(org.openqa.selenium.By)}
     */
    public void checkRegion(By selector, boolean stitchContent) {
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(selector);
        } else {
            checkRegion(selector);
        }
        this.stitchContent = false;
    }

    /**
     * See {@link #checkRegion(By, int, String)}.
     * Default match timeout is used.
     */
    public void checkRegion(By selector, String tag) {
        checkRegion(selector, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * If {@code stitchContent} is {@code false} then behaves the same as
     * {@link #checkRegion(org.openqa.selenium.By, String)}. Otherwise,
     * behaves the same as {@link #checkElement(By, String)}.
     */
    public void checkRegion(By selector, String tag, boolean stitchContent) {
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(selector, tag);
        } else {
            checkRegion(selector, tag);
        }
        this.stitchContent = false;
    }

    /**
     * Takes a snapshot of the application under test and matches a region
     * specified by the given selector with the expected region output.
     * @param selector     Selects the region to check.
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the screenshot.
     * @throws TestFailedException if a mismatch is detected and
     *                             immediate failure reports are enabled
     */
    public void checkRegion(By selector, int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckRegion(selector, %d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }

        checkRegion(getEyesDriver().findElement(selector), matchTimeout, tag);
    }

    /**
     * If {@code stitchContent} is {@code false} then behaves the same as
     * {@link #checkRegion(org.openqa.selenium.By, int, String)}. Otherwise,
     * behaves the same as {@link #checkElement(By, int, String)}.
     */
    public void checkRegion(By selector, int matchTimeout, String tag, boolean stitchContent) {
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(selector, matchTimeout, tag);
        } else {
            checkRegion(selector, matchTimeout, tag);
        }
        this.stitchContent = false;
    }

    /**
     * See {@link #checkRegionInFrame(int, By, String)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(int frameIndex, By selector) {
        checkRegionInFrame(frameIndex, selector, false);
    }

    /**
     * See {@link #checkRegionInFrame(int, By, String)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(int frameIndex, By selector, boolean stitchContent) {
        checkRegionInFrame(frameIndex, selector, null, stitchContent);
    }

    /**
     * See {@link #checkRegionInFrame(int, By, String, boolean)}.
     * {@code stitchContent} defaults to {@code false}.
     */
    public void checkRegionInFrame(int frameIndex, By selector, String tag) {
        checkRegionInFrame(frameIndex, selector, tag, false);
    }

    /**
     * See {@link #checkRegionInFrame(int, By, int, String, boolean)}.
     * Default match timeout is used.
     */
    public void checkRegionInFrame(int frameIndex, By selector, String tag, boolean stitchContent) {
        checkRegionInFrame(frameIndex, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag, stitchContent);
    }

    /**
     * See {@link #checkRegionInFrame(int, By, int, String, boolean)}.
     * {@code stitchContent} defaults to {@code false}.
     */
    public void checkRegionInFrame(int frameIndex, By selector, int matchTimeout, String tag) {
        checkRegionInFrame(frameIndex, selector, matchTimeout, tag, false);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     * @param frameIndex    The index of the frame to switch to. (The same index
     *                      as would be used in a call to
     *                      driver.switchTo().frame()).
     * @param selector      A Selector specifying the region to check.
     * @param matchTimeout  The amount of time to retry matching. (Milliseconds)
     * @param tag           An optional tag to be associated with the snapshot.
     * @param stitchContent If {@code true}, stitch the internal content of
     *                      the region (i.e., perform
     *                      {@link #checkElement(By, int, String)} on the
     *                      region.
     */
    public void checkRegionInFrame(int frameIndex, By selector,
                                   int matchTimeout, String tag,
                                   boolean stitchContent) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "CheckRegionInFrame(%d, selector, %d, '%s'): Ignored",
                    frameIndex, matchTimeout, tag));
            return;
        }

        getEyesDriver().switchTo().frame(frameIndex);
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(selector, matchTimeout, tag);
        } else {
            checkRegion(selector, matchTimeout, tag);
        }
        this.stitchContent = false;
        getEyesDriver().switchTo().parentFrame();
    }

    /**
     * See {@link #checkRegionInFrame(String, By, int, String, boolean)}.
     * {@code stitchContent} defaults to {@code null}.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector) {
        checkRegionInFrame(frameNameOrId, selector, false);
    }

    /**
     * See {@link #checkRegionInFrame(String, By, int, String, boolean)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector, boolean stitchContent) {
        checkRegionInFrame(frameNameOrId, selector, null, stitchContent);
    }

    /**
     * See {@link #checkRegionInFrame(String, By, int, String, boolean)}.
     * {@code stitchContent} defaults to {@code null}.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector,
                                   String tag) {
        checkRegionInFrame(frameNameOrId, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag, false);
    }

    /**
     * See {@link #checkRegionInFrame(String, By, int, String, boolean)}.
     * Default match timeout is used
     */
    public void checkRegionInFrame(String frameNameOrId, By selector,
                                   String tag, boolean stitchContent) {
        checkRegionInFrame(frameNameOrId, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag, stitchContent);
    }

    /**
     * See {@link #checkRegionInFrame(String, By, int, String, boolean)}.
     * {@code stitchContent} defaults to {@code false}.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector,
                                   int matchTimeout, String tag) {
        checkRegionInFrame(frameNameOrId, selector, matchTimeout, tag, false);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     * @param frameNameOrId The name or id of the frame to switch to. (as would
     *                      be used in a call to driver.switchTo().frame()).
     * @param selector      A Selector specifying the region to check.
     * @param matchTimeout  The amount of time to retry matching. (Milliseconds)
     * @param tag           An optional tag to be associated with the snapshot.
     * @param stitchContent If {@code true}, stitch the internal content of
     *                      the region (i.e., perform
     *                      {@link #checkElement(By, int, String)} on the region.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector,
                                   int matchTimeout, String tag,
                                   boolean stitchContent) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "CheckRegionInFrame('%s', selector, %d, '%s'): Ignored",
                    frameNameOrId, matchTimeout, tag));
            return;
        }
        getEyesDriver().switchTo().frame(frameNameOrId);
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(selector, matchTimeout, tag);
        } else {
            checkRegion(selector, matchTimeout, tag);
        }
        this.stitchContent = false;
        getEyesDriver().switchTo().parentFrame();
    }

    /**
     * See {@link #checkRegionInFrame(WebElement, By, boolean)}.
     * {@code stitchContent} defaults to {@code null}.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector) {
        checkRegionInFrame(frameReference, selector, false);
    }

    /**
     * See {@link #checkRegionInFrame(WebElement, By, String, boolean)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector, boolean stitchContent) {
        checkRegionInFrame(frameReference, selector, null, stitchContent);
    }

    /**
     * See {@link #checkRegionInFrame(WebElement, By, String, boolean)}.
     * {@code stitchContent} defaults to {@code false}.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector, String tag) {
        checkRegionInFrame(frameReference, selector, tag, false);
    }

    /**
     * See {@link #checkRegionInFrame(WebElement, By, int, String, boolean)}.
     * Default match timeout is used.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector,
                                   String tag, boolean stitchContent) {
        checkRegionInFrame(frameReference, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag, stitchContent);
    }

    /**
     * See {@link #checkRegionInFrame(WebElement, By, int, String, boolean)}.
     * {@code stitchContent} defaults to {@code false}.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector,
                                   int matchTimeout, String tag) {
        checkRegionInFrame(frameReference, selector, matchTimeout, tag, false);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     * @param frameReference The element which is the frame to switch to. (as
     *                       would be used in a call to
     *                       driver.switchTo().frame()).
     * @param selector       A Selector specifying the region to check.
     * @param matchTimeout   The amount of time to retry matching.
     *                       (Milliseconds)
     * @param tag            An optional tag to be associated with the snapshot.
     * @param stitchContent  If {@code true}, stitch the internal content of
     *                       the region (i.e., perform
     *                       {@link #checkElement(By, int, String)} on the
     *                       region.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector,
                                   int matchTimeout, String tag,
                                   boolean stitchContent) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "CheckRegionInFrame(frame, selector, %d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }
        getEyesDriver().switchTo().frame(frameReference);
        this.stitchContent = stitchContent;
        if (stitchContent) {
            checkElement(selector, matchTimeout, tag);
        } else {
            checkRegion(selector, matchTimeout, tag);
        }
        this.stitchContent = false;
        getEyesDriver().switchTo().parentFrame();
    }

    /**
     * Updates the state of scaling related parameters.
     */
    protected ScaleProviderFactory updateScalingParams() {
        // Update the scaling params only if we haven't done so yet, and the user hasn't set anything else manually.
        if (devicePixelRatio == UNKNOWN_DEVICE_PIXEL_RATIO &&
                scaleProviderHandler.get() instanceof NullScaleProvider) {
            ScaleProviderFactory factory;
            extractDevicePixelRatio();

            logger.verbose("Setting scale provider...");
            try {
                factory = getScaleProviderFactory();
            } catch (Exception e) {
                logger.verbose("Failed to set ContextBasedScaleProvider.");
                logger.verbose("Using FixedScaleProvider instead...");
                factory = new FixedScaleProviderFactory(1 / devicePixelRatio, scaleProviderHandler);
            }
            logger.verbose("Done!");
            return factory;
        }
        // If we already have a scale provider set, we'll just use it, and pass a mock as provider handler.
        PropertyHandler<ScaleProvider> nullProvider = new SimplePropertyHandler<>();
        return new ScaleProviderIdentityFactory(scaleProviderHandler.get(), nullProvider);
    }

    protected void extractDevicePixelRatio() {
        logger.verbose("Trying to extract device pixel ratio...");

        try {
            setDevicePixelRatio();
        } catch (Exception e) {
            logger.verbose(
                "Failed to extract device pixel ratio! Using default.");
            devicePixelRatio = DEFAULT_DEVICE_PIXEL_RATIO;
        }
        logger.verbose(String.format("Device pixel ratio: %f", devicePixelRatio));
    }

    protected void setDevicePixelRatio () {
        devicePixelRatio = EyesSeleniumUtils.getDevicePixelRatio(this.jsExecutor);
    }

    protected ScaleProviderFactory getScaleProviderFactory() {
        return new ContextBasedScaleProviderFactory(logger, getPositionProvider().getEntireSize(),
                viewportSizeHandler.get(), devicePixelRatio, false,
                scaleProviderHandler);
    }

    /**
     * Verifies the current frame.
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     */
    protected void checkCurrentFrame(int matchTimeout, String tag) {
        try {
            logger.verbose(String.format("CheckCurrentFrame(%d, '%s')", matchTimeout, tag));

            checkFrameOrElement = true;

            logger.verbose("Getting screenshot as base64..");
            String screenshot64 = getEyesDriver().getScreenshotAs(OutputType.BASE64);
            logger.verbose("Done! Creating image object...");
            BufferedImage screenshotImage = ImageUtils.imageFromBase64(screenshot64);

            // FIXME - Scaling should be handled in a single place instead
            ScaleProvider scaleProvider = updateScalingParams().getScaleProvider(screenshotImage.getWidth());

            screenshotImage = ImageUtils.scaleImage(screenshotImage, scaleProvider);
            logger.verbose("Done! Building required object...");
            final EyesWebDriverScreenshot screenshot = new EyesWebDriverScreenshot(logger, driver, screenshotImage);
            logger.verbose("Done!");

            logger.verbose("replacing regionToCheck");
            setRegionToCheck(screenshot.getFrameWindow());

            super.checkWindowBase(NullRegionProvider.INSTANCE, tag, false, matchTimeout);
        } finally {
            checkFrameOrElement = false;
            regionToCheck = null;
        }
    }

    /**
     * See {@link #checkFrame(String, int, String)}.
     * {@code tag} defaults to {@code null}. Default match timeout is used.
     */
    public void checkFrame(String frameNameOrId) {
        checkFrame(frameNameOrId, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * See {@link #checkFrame(String, int, String)}.
     * Default match timeout is used.
     */
    public void checkFrame(String frameNameOrId, String tag) {
        checkFrame(frameNameOrId, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Matches the frame given as parameter, by switching into the frame and
     * using stitching to get an image of the frame.
     * @param frameNameOrId The name or id of the frame to check. (The same
     *                      name/id as would be used in a call to
     *                      getEyesDriver().switchTo().frame()).
     * @param matchTimeout  The amount of time to retry matching. (Milliseconds)
     * @param tag           An optional tag to be associated with the match.
     */
    public void checkFrame(String frameNameOrId, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("CheckFrame(%s, %d, '%s'): Ignored",
                    frameNameOrId, matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(frameNameOrId, "frameNameOrId");

        logger.log(String.format("CheckFrame(%s, %d, '%s')", frameNameOrId, matchTimeout, tag));

        check(tag, Target.frame(frameNameOrId).timeout(matchTimeout).fully());

        logger.verbose("Done!");
    }

    /**
     * See {@link #checkFrame(int, int, String)}.
     * {@code tag} defaults to {@code null}. Default match timeout is used.
     */
    public void checkFrame(int frameIndex) {
        checkFrame(frameIndex, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * See {@link #checkFrame(int, int, String)}.
     * Default match timeout is used.
     */
    public void checkFrame(int frameIndex, String tag) {
        checkFrame(frameIndex, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Matches the frame given as parameter, by switching into the frame and
     * using stitching to get an image of the frame.
     * @param frameIndex   The index of the frame to switch to. (The same index
     *                     as would be used in a call to
     *                     getEyesDriver().switchTo().frame()).
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the match.
     */
    public void checkFrame(int frameIndex, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("CheckFrame(%d, %d, '%s'): Ignored", frameIndex, matchTimeout, tag));
            return;
        }

        ArgumentGuard.greaterThanOrEqualToZero(frameIndex, "frameIndex");

        logger.log(String.format("CheckFrame(%d, %d, '%s')", frameIndex, matchTimeout, tag));

        check(tag, Target.frame(frameIndex).timeout(matchTimeout).fully());
    }

    /**
     * See {@link #checkFrame(WebElement, int, String)}.
     * {@code tag} defaults to {@code null}.
     * Default match timeout is used.
     */
    public void checkFrame(WebElement frameReference) {
        checkFrame(frameReference, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * See {@link #checkFrame(WebElement, int, String)}.
     * Default match timeout is used.
     */
    public void checkFrame(WebElement frameReference, String tag) {
        checkFrame(frameReference, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Matches the frame given as parameter, by switching into the frame and
     * using stitching to get an image of the frame.
     * @param frameReference The element which is the frame to switch to. (as
     *                       would be used in a call to
     *                       getEyesDriver().switchTo().frame() ).
     * @param matchTimeout   The amount of time to retry matching (milliseconds).
     * @param tag            An optional tag to be associated with the match.
     */
    public void checkFrame(WebElement frameReference, int matchTimeout,
                           String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("checkFrame(element, %d, '%s'): Ignored", matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(frameReference, "frameReference");

        logger.log(String.format("CheckFrame(element, %d, '%s')", matchTimeout, tag));

        logger.verbose("Switching to frame based on element reference...");
        getEyesDriver().switchTo().frame(frameReference);
        logger.verbose("Done!");

        checkCurrentFrame(matchTimeout, tag);

        logger.verbose("Switching back to parent frame...");
        getEyesDriver().switchTo().parentFrame();
        logger.verbose("Done!");
    }

    /**
     * Matches the frame given by the frames path, by switching into the frame
     * and using stitching to get an image of the frame.
     * @param framePath    The path to the frame to check. This is a list of
     *                     frame names/IDs (where each frame is nested in the
     *                     previous frame).
     * @param matchTimeout The amount of time to retry matching (milliseconds).
     * @param tag          An optional tag to be associated with the match.
     */
    public void checkFrame(String[] framePath, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "checkFrame(framePath, %d, '%s'): Ignored",
                    matchTimeout,
                    tag));
            return;
        }
        ArgumentGuard.notNull(framePath, "framePath");
        ArgumentGuard.greaterThanZero(framePath.length, "framePath.length");
        logger.log(String.format(
                "checkFrame(framePath, %d, '%s')", matchTimeout, tag));
        FrameChain originalFrameChain = getEyesDriver().getFrameChain();
        // We'll switch into the PARENT frame of the frame we want to check,
        // and call check frame.
        logger.verbose("Switching to parent frame according to frames path..");
        String[] parentFramePath = new String[framePath.length - 1];
        System.arraycopy(framePath, 0, parentFramePath, 0,
                parentFramePath.length);
        ((EyesTargetLocator) (getEyesDriver().switchTo())).frames(parentFramePath);
        logger.verbose("Done! Calling checkFrame..");
        checkFrame(framePath[framePath.length - 1], matchTimeout, tag);
        logger.verbose("Done! switching to default content..");
        getEyesDriver().switchTo().defaultContent();
        logger.verbose("Done! Switching back into the original frame..");
        ((EyesTargetLocator) (getEyesDriver().switchTo())).frames(originalFrameChain);
        logger.verbose("Done!");
    }

    /**
     * See {@link #checkFrame(String[], int, String)}.
     * Default match timeout is used.
     */
    public void checkFrame(String[] framesPath, String tag) {
        checkFrame(framesPath, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * See {@link #checkFrame(String[], int, String)}.
     * Default match timeout is used.
     * {@code tag} defaults to {@code null}.
     */
    public void checkFrame(String[] framesPath) {
        checkFrame(framesPath, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     * @param framePath     The path to the frame to check. This is a list of
     *                      frame names/IDs (where each frame is nested in the previous frame).
     * @param selector      A Selector specifying the region to check.
     * @param matchTimeout  The amount of time to retry matching (milliseconds).
     * @param tag           An optional tag to be associated with the snapshot.
     * @param stitchContent Whether or not to stitch the internal content of the
     *                      region (i.e., perform {@link #checkElement(By, int, String)} on the region.
     */
    public void checkRegionInFrame(String[] framePath, By selector,
                                   int matchTimeout, String tag,
                                   boolean stitchContent) {
        if (getIsDisabled()) {
            logger.log(String.format("checkRegionInFrame(framePath, selector, %d, '%s'): Ignored", matchTimeout, tag));
            return;
        }
        ArgumentGuard.notNull(framePath, "framePath");
        ArgumentGuard.greaterThanZero(framePath.length, "framePath.length");
        logger.log(String.format(
                "checkFrame(framePath, %d, '%s')", matchTimeout, tag));
        FrameChain originalFrameChain = getEyesDriver().getFrameChain();
        // We'll switch into the PARENT frame of the frame we want to check,
        // and call check frame.
        logger.verbose("Switching to parent frame according to frames path..");
        String[] parentFramePath = new String[framePath.length - 1];
        System.arraycopy(framePath, 0, parentFramePath, 0, parentFramePath.length);
        ((EyesTargetLocator) (getEyesDriver().switchTo())).frames(parentFramePath);
        logger.verbose("Done! Calling checkRegionInFrame..");
        checkRegionInFrame(framePath[framePath.length - 1], selector, matchTimeout, tag, stitchContent);
        logger.verbose("Done! switching back to default content..");
        getEyesDriver().switchTo().defaultContent();
        logger.verbose("Done! Switching into the original frame..");
        ((EyesTargetLocator) (getEyesDriver().switchTo())).frames(originalFrameChain);
        logger.verbose("Done!");
    }

    /**
     * See {@link #checkRegionInFrame(String[], By, int, String, boolean)}.
     * {@code stitchContent} defaults to {@code false}.
     */
    public void checkRegionInFrame(String[] framePath, By selector, int matchTimeout, String tag) {
        checkRegionInFrame(framePath, selector, matchTimeout, tag, false);
    }

    /**
     * See {@link #checkRegionInFrame(String[], By, int, String)}.
     * Default match timeout is used.
     */
    public void checkRegionInFrame(String[] framePath, By selector, String tag) {
        checkRegionInFrame(framePath, selector, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * See {@link #checkRegionInFrame(String[], By, int, String)}.
     * Default match timeout is used.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(String[] framePath, By selector) {
        checkRegionInFrame(framePath, selector, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * See {@link #checkElement(WebElement, String)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkElement(WebElement element) {
        checkElement(element, null);
    }

    /**
     * See {@link #checkElement(WebElement, int, String)}.
     * Default match timeout is used.
     */
    public void checkElement(WebElement element, String tag) {
        checkElement(element, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    private void checkElement(String name, ICheckSettings checkSettings) {
        this.checkElement(this.targetElement, name, checkSettings);
    }

    private void checkElement(WebElement element, String name, ICheckSettings checkSettings) {

        // Since the element might already have been found using EyesWebDriver.
        final EyesRemoteWebElement eyesElement = driver.getEyesElement(element);

        this.regionToCheck = null;

        PositionProvider originalPositionProvider = getPositionProvider();
        PositionProvider scrollPositionProvider = getScrollPositionProvider();
        Location originalScrollPosition = scrollPositionProvider.getCurrentPosition();

        String originalOverflow = null;

        Point pl = eyesElement.getLocation();

        try {
            checkFrameOrElement = true;

            String displayStyle = eyesElement.getComputedStyle("display");
            if (!displayStyle.equals("inline")) {
                elementPositionProvider = new ElementPositionProvider(logger, driver, eyesElement);
            }

            if (hideScrollbars) {
                // Set overflow to "hidden".
                originalOverflow = eyesElement.getOverflow();
                eyesElement.setOverflow("hidden");
            }

            int elementWidth = eyesElement.getClientWidth();
            int elementHeight = eyesElement.getClientHeight();

            int borderLeftWidth = eyesElement.getComputedStyleInteger("border-left-width");
            int borderTopWidth = eyesElement.getComputedStyleInteger("border-top-width");

            final Region elementRegion = new Region(
                    pl.getX() + borderLeftWidth, pl.getY() + borderTopWidth,
                    elementWidth, elementHeight, CoordinatesType.CONTEXT_RELATIVE);

            logger.verbose("Element region: " + elementRegion);

            logger.verbose("replacing regionToCheck");
            regionToCheck = elementRegion;

            checkWindowBase(NullRegionProvider.INSTANCE, name, false, checkSettings);
        } finally {
            if (originalOverflow != null) {
                eyesElement.setOverflow(originalOverflow);
            }

            checkFrameOrElement = false;

            scrollPositionProvider.setPosition(originalScrollPosition);
            positionProvider = originalPositionProvider;
            regionToCheck = null;
            elementPositionProvider = null;
        }
    }

    /**
     * Takes a snapshot of the application under test and matches a specific
     * element with the expected region output.
     * @param element      The element to check.
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     * @throws TestFailedException if a mismatch is detected and immediate failure reports are enabled
     */
    public void checkElement(WebElement element, int matchTimeout, String tag) {
        checkElement(element, tag, Target.region(element).timeout(matchTimeout));
    }

    /**
     * See {@link #checkElement(By, String)}.
     * {@code tag} defaults to {@code null}.
     */
    public void checkElement(By selector) {
        checkElement(selector, null);
    }

    /**
     * See {@link #checkElement(By, int, String)}.
     * Default match timeout is used.
     */
    public void checkElement(By selector, String tag) {
        checkElement(selector, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Takes a snapshot of the application under test and matches an element
     * specified by the given selector with the expected region output.
     * @param selector     Selects the element to check.
     * @param matchTimeout The amount of time to retry matching. (Milliseconds)
     * @param tag          An optional tag to be associated with the screenshot.
     * @throws TestFailedException if a mismatch is detected and
     *                             immediate failure reports are enabled
     */
    public void checkElement(By selector, int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckElement(selector, %d, '%s'): Ignored", matchTimeout, tag));
            return;
        }

        checkElement(getEyesDriver().findElement(selector), matchTimeout, tag);
    }

    /**
     * Adds a mouse trigger.
     * @param action  Mouse action.
     * @param control The control on which the trigger is activated (context relative coordinates).
     * @param cursor  The cursor's position relative to the control.
     */
    public void addMouseTrigger(MouseAction action, Region control, Location cursor) {
        if (getIsDisabled()) {
            logger.verbose(String.format("Ignoring %s (disabled)", action));
            return;
        }

        // Triggers are actually performed on the previous window.
        if (lastScreenshot == null) {
            logger.verbose(String.format("Ignoring %s (no screenshot)", action));
            return;
        }

        if (!FrameChain.isSameFrameChain(getEyesDriver().getFrameChain(),
                ((EyesWebDriverScreenshot) lastScreenshot).getFrameChain())) {
            logger.verbose(String.format("Ignoring %s (different frame)", action));
            return;
        }

        addMouseTriggerBase(action, control, cursor);
    }

    /**
     * Adds a mouse trigger.
     * @param action  Mouse action.
     * @param element The WebElement on which the click was called.
     */
    public void addMouseTrigger(MouseAction action, WebElement element) {
        if (getIsDisabled()) {
            logger.verbose(String.format("Ignoring %s (disabled)", action));
            return;
        }

        ArgumentGuard.notNull(element, "element");

        Point pl = element.getLocation();
        Dimension ds = element.getSize();

        Region elementRegion = new Region(pl.getX(), pl.getY(), ds.getWidth(),
                ds.getHeight());

        // Triggers are actually performed on the previous window.
        if (lastScreenshot == null) {
            logger.verbose(String.format("Ignoring %s (no screenshot)", action));
            return;
        }

        if (!FrameChain.isSameFrameChain(getEyesDriver().getFrameChain(),
                ((EyesWebDriverScreenshot) lastScreenshot).getFrameChain())) {
            logger.verbose(String.format("Ignoring %s (different frame)", action));
            return;
        }

        // Get the element region which is intersected with the screenshot,
        // so we can calculate the correct cursor position.
        elementRegion = lastScreenshot.getIntersectedRegion
                (elementRegion, CoordinatesType.CONTEXT_RELATIVE);

        addMouseTriggerBase(action, elementRegion,
                elementRegion.getMiddleOffset());
    }

    /**
     * Adds a keyboard trigger.
     * @param control The control's context-relative region.
     * @param text    The trigger's text.
     */
    public void addTextTrigger(Region control, String text) {
        if (getIsDisabled()) {
            logger.verbose(String.format("Ignoring '%s' (disabled)", text));
            return;
        }

        if (lastScreenshot == null) {
            logger.verbose(String.format("Ignoring '%s' (no screenshot)", text));
            return;
        }

        if (!FrameChain.isSameFrameChain(getEyesDriver().getFrameChain(),
                ((EyesWebDriverScreenshot) lastScreenshot).getFrameChain())) {
            logger.verbose(String.format("Ignoring '%s' (different frame)", text));
            return;
        }

        addTextTriggerBase(control, text);
    }

    /**
     * Adds a keyboard trigger.
     * @param element The element for which we sent keys.
     * @param text    The trigger's text.
     */
    public void addTextTrigger(WebElement element, String text) {
        if (getIsDisabled()) {
            logger.verbose(String.format("Ignoring '%s' (disabled)", text));
            return;
        }

        ArgumentGuard.notNull(element, "element");

        Point pl = element.getLocation();
        Dimension ds = element.getSize();

        Region elementRegion = new Region(pl.getX(), pl.getY(), ds.getWidth(), ds.getHeight());

        addTextTrigger(elementRegion, text);
    }

    /**
     * Use this method only if you made a previous call to {@link #open
     * (WebDriver, String, String)} or one of its variants.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public RectangleSize getViewportSize() {
        RectangleSize viewportSize = viewportSizeHandler.get();
        if (viewportSize == null) {
            viewportSize = getEyesDriver().getDefaultContentViewportSize();
        }
        return viewportSize;
    }

    /**
     * Call this method if for some
     * reason you don't want to call {@link #open(WebDriver, String, String)}
     * (or one of its variants) yet.
     * @param driver The driver to use for getting the viewport.
     * @return The viewport size of the current context.
     */
    public static RectangleSize getViewportSize(WebDriver driver) {
        ArgumentGuard.notNull(driver, "driver");
        return EyesSeleniumUtils.getViewportSizeOrDisplaySize(new Logger(), driver);
    }

    /**
     * Use this method only if you made a previous call to {@link #open
     * (WebDriver, String, String)} or one of its variants.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void setViewportSize(RectangleSize size) {
        if (viewportSizeHandler instanceof ReadOnlyPropertyHandler) {
            logger.verbose("Ignored (viewport size given explicitly)");
            return;
        }

        FrameChain originalFrame = getEyesDriver().getFrameChain();
        getEyesDriver().switchTo().defaultContent();

        try {
            EyesSeleniumUtils.setViewportSize(logger, driver, size);
        } catch (EyesException e) {
            // Just in case the user catches this error
            ((EyesTargetLocator) getEyesDriver().switchTo()).frames(originalFrame);

            throw new TestFailedException("Failed to set the viewport size", e);
        }
        ((EyesTargetLocator) getEyesDriver().switchTo()).frames(originalFrame);
        viewportSizeHandler.set(new RectangleSize(size.getWidth(), size.getHeight()));
    }

    /**
     * Set the viewport size using the driver. Call this method if for some
     * reason you don't want to call {@link #open(WebDriver, String, String)}
     * (or one of its variants) yet.
     * @param driver The driver to use for setting the viewport.
     * @param size   The required viewport size.
     */
    public static void setViewportSize(WebDriver driver, RectangleSize size) {
        ArgumentGuard.notNull(driver, "driver");
        EyesSeleniumUtils.setViewportSize(new Logger(), driver, size);
    }

    @Override
    protected void beforeOpen() {
        tryHideScrollbars();
    }

    @Override
    protected void beforeMatchWindow() {
        tryHideScrollbars();
    }

    private void tryHideScrollbars() {
        if (this.hideScrollbars) {
            FrameChain originalFC = new FrameChain(logger, driver.getFrameChain());
            FrameChain fc = new FrameChain(logger, driver.getFrameChain());
            EyesSeleniumUtils.hideScrollbars(this.driver, 200);
            while (fc.size() > 0) {
                driver.getRemoteWebDriver().switchTo().parentFrame();
                Frame frame = fc.pop();
                EyesSeleniumUtils.hideScrollbars(this.driver, 200);
            }
            ((EyesTargetLocator) driver.switchTo()).frames(originalFC);
        }
    }

    private void tryRestoreScrollbars() {
        if (this.hideScrollbars) {
            FrameChain originalFC = new FrameChain(logger, driver.getFrameChain());
            FrameChain fc = new FrameChain(logger, driver.getFrameChain());
            while (fc.size() > 0) {
                driver.getRemoteWebDriver().switchTo().parentFrame();
                Frame frame = fc.pop();
                ((EyesRemoteWebElement)frame.getReference()).setOverflow(frame.getOriginalOverflow());
            }
            ((EyesTargetLocator) driver.switchTo()).frames(originalFC);
        }
    }

    /*
    @Override
    protected void afterMatchWindow() {
        if (this.hideScrollbars) {
            try {
                EyesSeleniumUtils.setOverflow(this.driver, this.originalOverflow);
            } catch (EyesDriverOperationException e) {
                // Bummer, but we'll continue with the screenshot anyway :)
                logger.log("WARNING: Failed to revert overflow! Error: " + e.getMessage());
            }
        }
    }
    */

    protected ScrollingPositionProvider getScrollPositionProvider () {
        return new ScrollPositionProvider(logger, this.jsExecutor);
    }

    @Override
    protected EyesScreenshot getScreenshot() {

        logger.verbose("getScreenshot()");
        EyesWebDriverScreenshot result;

        if (checkFrameOrElement) {
            result = getFrameOrElementScreenshot();
        } else if (forceFullPageScreenshot || stitchContent) {
            result = getFullPageScreenshot();
        } else {
            result = getSimpleScreenshot();

        }
        logger.verbose("Done!");
        return result;
    }

    protected EyesWebDriverScreenshot getFullPageScreenshot () {
        logger.verbose("Full page screenshot requested.");

        EyesScreenshotFactory screenshotFactory = new EyesWebDriverScreenshotFactory(logger, getEyesDriver());
        ScaleProviderFactory scaleProviderFactory = updateScalingParams();

        FullPageCaptureAlgorithm algo = new FullPageCaptureAlgorithm(logger,
            getScrollPositionProvider(), getPositionProvider(), getScrollPositionProvider(),
            imageProvider, debugScreenshotsProvider, scaleProviderFactory, cutProviderHandler.get(),
            screenshotFactory, getWaitBeforeScreenshots());
        EyesTargetLocator switchTo = (EyesTargetLocator) getEyesDriver().switchTo();
        FrameChain originalFrameChain = new FrameChain(logger, getEyesDriver().getFrameChain());
        // Save the current frame path.
        Location originalFramePosition = originalFrameChain.size() > 0 ? originalFrameChain.getDefaultContentScrollPosition() : new Location(0, 0);

        switchTo.defaultContent();

        BufferedImage fullPageImage =
            algo.getStitchedRegion(Region.EMPTY, getStitchOverlap(), regionPositionCompensation);

        switchTo.frames(originalFrameChain);
        return new EyesWebDriverScreenshot(logger, driver, fullPageImage, null,
            originalFramePosition, getScrollPositionProvider());
    }

    protected EyesWebDriverScreenshot getFrameOrElementScreenshot() {
        EyesScreenshotFactory screenshotFactory = new EyesWebDriverScreenshotFactory(logger, getEyesDriver());
        ScaleProviderFactory scaleProviderFactory = updateScalingParams();

        FrameChain originalFrameChain = new FrameChain(logger, getEyesDriver().getFrameChain());
        FullPageCaptureAlgorithm algo = new FullPageCaptureAlgorithm(logger,
            getPositionProvider(), getElementPositionProvider(), getScrollPositionProvider(),
            imageProvider, debugScreenshotsProvider, scaleProviderFactory, cutProviderHandler.get(),
            screenshotFactory, getWaitBeforeScreenshots());
        EyesTargetLocator switchTo = (EyesTargetLocator) getEyesDriver().switchTo();

        logger.verbose("Check frame/element requested");

        switchTo.framesDoScroll(originalFrameChain);

        BufferedImage entireFrameOrElement =
            algo.getStitchedRegion(regionToCheck, getStitchOverlap(), regionPositionCompensation);

        logger.verbose("Building screenshot object...");
        return new EyesWebDriverScreenshot(logger, driver, entireFrameOrElement,
            new RectangleSize(entireFrameOrElement.getWidth(), entireFrameOrElement.getHeight()));
    }

    protected EyesWebDriverScreenshot getSimpleScreenshot() {
        ScaleProviderFactory scaleProviderFactory = updateScalingParams();
        ensureElementVisible(this.targetElement);

        logger.verbose("Screenshot requested...");
        BufferedImage screenshotImage = imageProvider.getImage();
        debugScreenshotsProvider.save(screenshotImage, "original");

        ScaleProvider scaleProvider = scaleProviderFactory.getScaleProvider(screenshotImage.getWidth());
        if (scaleProvider.getScaleRatio() != 1.0) {
            logger.verbose("scaling...");
            screenshotImage = ImageUtils.scaleImage(screenshotImage, scaleProvider);
            debugScreenshotsProvider.save(screenshotImage, "scaled");
        }

        CutProvider cutProvider = cutProviderHandler.get();
        if (!(cutProvider instanceof NullCutProvider)) {
            logger.verbose("cutting...");
            screenshotImage = cutProvider.cut(screenshotImage);
            debugScreenshotsProvider.save(screenshotImage, "cut");
        }

        logger.verbose("Creating screenshot object...");
        return new EyesWebDriverScreenshot(logger, driver, screenshotImage);
    }

    @Override
    protected String getTitle() {
        if (!dontGetTitle) {
            try {
                return getEyesDriver().getTitle();
            } catch (Exception ex) {
                logger.verbose("failed (" + ex.getMessage() + ")");
                dontGetTitle = true;
            }
        }

        return "";
    }

    @Override
    protected String getInferredEnvironment() {
        String userAgent = getEyesDriver().getUserAgent();
        if (userAgent != null) {
            return "useragent:" + userAgent;
        }

        return null;
    }

    /**
     * @return The currently set position provider.
     */
    public PositionProvider getElementPositionProvider() {
        return elementPositionProvider == null ? getPositionProvider() : elementPositionProvider;
    }

}
