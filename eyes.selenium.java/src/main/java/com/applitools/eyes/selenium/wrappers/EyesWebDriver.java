package com.applitools.eyes.selenium.wrappers;

import com.applitools.eyes.EyesException;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.EyesSeleniumUtils;
import com.applitools.eyes.selenium.frames.FrameChain;
import com.applitools.eyes.selenium.positioning.ImageRotation;
import com.applitools.eyes.selenium.triggers.EyesKeyboard;
import com.applitools.eyes.selenium.triggers.EyesMouse;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.ImageUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.internal.*;
import org.openqa.selenium.remote.*;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * An Eyes implementation of the interfaces implemented by
 * {@link org.openqa.selenium.remote.RemoteWebDriver}.
 * Used so we'll be able to return the users an object with the same
 * functionality as {@link org.openqa.selenium.remote.RemoteWebDriver}.
 */
public class EyesWebDriver implements HasCapabilities, HasInputDevices,
        FindsByClassName, FindsByCssSelector, FindsById, FindsByLinkText,
        FindsByName, FindsByTagName, FindsByXPath, JavascriptExecutor,
        SearchContext, TakesScreenshot, WebDriver, HasTouchScreen {

    protected Logger logger;
    protected Eyes eyes;
    protected RemoteWebDriver driver;
    protected TouchScreen touch;
    protected Map<String, WebElement> elementsIds;
    protected FrameChain frameChain;

    protected ImageRotation rotation;
    protected RectangleSize defaultContentViewportSize;

    /**
     * Rotates the image as necessary. The rotation is either manually forced
     * by passing a non-null ImageRotation, or automatically inferred.
     * @param driver   The underlying driver which produced the screenshot.
     * @param image    The image to normalize.
     * @param rotation The degrees by which to rotate the image:
     *                 positive values = clockwise rotation,
     *                 negative values = counter-clockwise,
     *                 0 = force no rotation,
     *                 null = rotate automatically as needed.
     * @return A normalized image.
     */
    public BufferedImage normalizeRotation(Logger logger, WebDriver driver,
                                           BufferedImage image, ImageRotation rotation) {
        ArgumentGuard.notNull(driver, "driver");
        ArgumentGuard.notNull(image, "image");
        int degrees;
        if (rotation != null) {
            degrees = rotation.getRotation();
        } else {
            degrees = EyesSeleniumUtils.tryAutomaticRotation(logger, driver, image);
        }

        BufferedImage normalizedImage = ImageUtils.rotateImage(image, degrees);

        return normalizedImage;
    }

    public EyesWebDriver(Logger logger, Eyes eyes, RemoteWebDriver driver)
            throws EyesException {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(eyes, "eyes");
        ArgumentGuard.notNull(driver, "driver");

        this.logger = logger;
        this.eyes = eyes;
        this.driver = driver;

        this.elementsIds = new HashMap<>();
        this.frameChain = new FrameChain(logger);
        this.defaultContentViewportSize = null;

        // initializing "touch" if possible
        ExecuteMethod executeMethod = null;
        try {
            executeMethod = new RemoteExecuteMethod(driver);
        } catch (Exception e) {
            // If an exception occurred, we simply won't instantiate "touch".
        }
        if (null != executeMethod) {
            touch = new EyesTouchScreen(logger, this,
                    new RemoteTouchScreen(executeMethod));
        } else {
            touch = null;
        }

        logger.verbose("Driver session is " + getSessionId());
    }

    public Eyes getEyes() {
        return eyes;
    }

    public RemoteWebDriver getRemoteWebDriver() {
        return driver;
    }

    public TouchScreen getTouch() {
        return touch;
    }

    @SuppressWarnings("unused")
    /**
     *
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
    }

    public void get(String s) {
        frameChain.clear();
        driver.get(s);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public List<WebElement> findElements(By by) {
        List<WebElement> foundWebElementsList = driver.findElements(by);

        // This list will contain the found elements wrapped with our class.
        List<WebElement> resultElementsList =
                new ArrayList<WebElement>(foundWebElementsList.size());

        for (WebElement currentElement : foundWebElementsList) {
            if (currentElement instanceof RemoteWebElement) {
                resultElementsList.add(new EyesRemoteWebElement(logger, this,
                        (RemoteWebElement) currentElement));

                // For Remote web elements, we can keep the IDs
                elementsIds.put(((RemoteWebElement) currentElement).getId(),
                        currentElement);

            } else {
                throw new EyesException(String.format(
                        "findElements: element is not a RemoteWebElement: %s",
                        by));
            }
        }

        return resultElementsList;
    }

    public WebElement findElement(By by) {
        WebElement webElement = driver.findElement(by);
        if (webElement instanceof RemoteWebElement) {
            webElement = new EyesRemoteWebElement(logger, this, webElement);

            // For Remote web elements, we can keep the IDs,
            // for Id based lookup (mainly used for Javascript related
            // activities).
            elementsIds.put(((RemoteWebElement) webElement).getId(), webElement);
        } else {
            throw new EyesException("findElement: Element is not a RemoteWebElement: " + by);
        }

        return webElement;
    }

    /**
     * Found elements are sometimes accessed by their IDs (e.g. tapping an
     * element in Appium).
     * @return Maps of IDs for found elements.
     */
    @SuppressWarnings("UnusedDeclaration")
    public Map<String, WebElement> getElementIds() {
        return elementsIds;
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public void close() {
        driver.close();
    }

    public void quit() {
        driver.quit();
    }

    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    public TargetLocator switchTo() {
        logger.verbose("switchTo()");
        return new EyesTargetLocator(logger, this, driver.switchTo());
    }

    public Navigation navigate() {
        return driver.navigate();
    }

    public Options manage() {
        return driver.manage();
    }

    public Mouse getMouse() {
        return new EyesMouse(logger, this, driver.getMouse());
    }

    public Keyboard getKeyboard() {
        return new EyesKeyboard(logger, this, driver.getKeyboard());
    }

    public WebElement findElementByClassName(String className) {
        return findElement(By.className(className));
    }

    public List<WebElement> findElementsByClassName(String className) {
        return findElements(By.className(className));
    }

    public WebElement findElementByCssSelector(String cssSelector) {
        return findElement(By.cssSelector(cssSelector));
    }

    public List<WebElement> findElementsByCssSelector(String cssSelector) {
        return findElements(By.cssSelector(cssSelector));
    }

    public WebElement findElementById(String id) {
        return findElement(By.id(id));
    }

    public List<WebElement> findElementsById(String id) {
        return findElements(By.id(id));
    }

    public WebElement findElementByLinkText(String linkText) {
        return findElement(By.linkText(linkText));
    }

    public List<WebElement> findElementsByLinkText(String linkText) {
        return findElements(By.linkText(linkText));
    }

    public WebElement findElementByPartialLinkText(String partialLinkText) {
        return findElement(By.partialLinkText(partialLinkText));
    }

    public List<WebElement> findElementsByPartialLinkText(String partialLinkText) {
        return findElements(By.partialLinkText(partialLinkText));
    }

    public WebElement findElementByName(String name) {
        return findElement(By.name(name));
    }

    public List<WebElement> findElementsByName(String name) {
        return findElements(By.name(name));
    }

    public WebElement findElementByTagName(String tagName) {
        return findElement(By.tagName(tagName));
    }

    public List<WebElement> findElementsByTagName(String tagName) {
        return findElements(By.tagName(tagName));
    }

    public WebElement findElementByXPath(String path) {
        return findElement(By.xpath(path));
    }

    public List<WebElement> findElementsByXPath(String path) {
        return findElements(By.xpath(path));
    }

    public Capabilities getCapabilities() {
        return driver.getCapabilities();
    }

    public Object executeScript(String script, Object... args) {
        EyesSeleniumUtils.handleSpecialCommands(script, args);
        Object result = driver.executeScript(script, args);
        return result;
    }

    public Object executeAsyncScript(String script, Object... args) {
        EyesSeleniumUtils.handleSpecialCommands(script, args);
        Object result = driver.executeAsyncScript(script, args);
        return result;
    }

    /**
     * @param forceQuery If true, we will perform the query even if we have a cached viewport size.
     * @return The viewport size of the default content (outer most frame).
     */
    public RectangleSize getDefaultContentViewportSize(boolean forceQuery) {
        logger.verbose("getDefaultContentViewportSize(forceQuery: " + forceQuery + ")");

        if (defaultContentViewportSize != null && !forceQuery) {
            logger.verbose("Using cached viewport size: " + defaultContentViewportSize);
            return defaultContentViewportSize;
        }

        EyesTargetLocator switchTo = (EyesTargetLocator)switchTo();
        FrameChain currentFrames = new FrameChain(logger, getFrameChain());

        // Optimization
        if (currentFrames.size() > 0) {
            switchTo.defaultContent();
        }

        logger.verbose("Extracting viewport size...");
        defaultContentViewportSize = EyesSeleniumUtils.getViewportSizeOrDisplaySize(logger, this);
        logger.verbose("Done! Viewport size: " + defaultContentViewportSize);

        if (currentFrames.size() > 0) {
            switchTo.frames(currentFrames);
        }
        return defaultContentViewportSize;
    }

    /**
     * See {@link #getDefaultContentViewportSize(boolean)}.
     * {@code forceQuery} defaults to {@code false}.
     */
    public RectangleSize getDefaultContentViewportSize() {
        return getDefaultContentViewportSize(true);
    }

    /**
     * @return The current frame chain.
     */
    public FrameChain getFrameChain() {
        return frameChain;
    }

    public <X> X getScreenshotAs(OutputType<X> xOutputType)
            throws WebDriverException {
        // Get the image as base64.
        String screenshot64 = driver.getScreenshotAs(OutputType.BASE64);
        BufferedImage screenshot = ImageUtils.imageFromBase64(screenshot64);
        screenshot = normalizeRotation(logger, driver, screenshot, rotation);

        // Return the image in the requested format.
        screenshot64 = ImageUtils.base64FromImage(screenshot);
        return xOutputType.convertFromBase64Png(screenshot64);
    }

    public String getUserAgent() {
        String userAgent;
        try {
            userAgent = (String) this.driver.executeScript("return navigator.userAgent");
            logger.verbose("user agent: " + userAgent);
        } catch (Exception e) {
            logger.verbose("Failed to obtain user-agent string");
            userAgent = null;
        }

        return userAgent;
    }

    private String getSessionId() {
        // extract remote web driver information
        return driver.getSessionId().toString();
    }
}
