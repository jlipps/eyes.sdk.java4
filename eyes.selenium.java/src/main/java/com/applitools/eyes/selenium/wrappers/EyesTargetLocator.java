package com.applitools.eyes.selenium.wrappers;

import com.applitools.eyes.EyesException;
import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.positioning.ScrollingPositionProvider;
import com.applitools.eyes.selenium.frames.Frame;
import com.applitools.eyes.selenium.frames.FrameChain;
import com.applitools.utils.ArgumentGuard;
import java.util.List;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public abstract class EyesTargetLocator implements WebDriver.TargetLocator {

    protected Logger logger;
    protected WebDriver.TargetLocator targetLocator;

    /**
     * Initialized a new EyesTargetLocator object.
     * @param logger        The logger object
     * @param targetLocator The actual TargetLocator object.
     */
    public EyesTargetLocator(Logger logger, WebDriver.TargetLocator targetLocator) {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(targetLocator, "targetLocator");
        this.logger = logger;
        this.targetLocator = targetLocator;
    }

    protected abstract void switchToFrame(String nameOrId);
    protected abstract void switchToFrame(WebElement frame);
    protected abstract void switchToParent();
    protected abstract void switchToDefault();
    protected abstract void switchToWindow(String nameOrHandle);
    protected abstract EyesWebDriver getDriver();
    protected abstract ScrollingPositionProvider getScrollProvider();
    protected abstract WebElement getFrameByIndex(int index);
    protected abstract List<WebElement> getFramesByName(String name);

    /**
     * Will be called before switching into a frame.
     * @param targetFrame The element about to be switched to.
     */
    protected void willSwitchToFrame(WebElement targetFrame) {

        ArgumentGuard.notNull(targetFrame, "targetFrame");

        EyesRemoteWebElement eyesFrame = getDriver().getEyesElement(targetFrame);

        Point pl = targetFrame.getLocation();
        Dimension ds = targetFrame.getSize();

        int clientWidth = eyesFrame.getClientWidth();
        int clientHeight = eyesFrame.getClientHeight();

        int borderLeftWidth = eyesFrame.getComputedStyleInteger("border-left-width");
        int borderTopWidth = eyesFrame.getComputedStyleInteger("border-top-width");

        Location contentLocation = new Location(pl.getX() + borderLeftWidth, pl.getY() + borderTopWidth);

        Location originalLocation = getScrollProvider().getCurrentPosition();

        String originalOverflow = eyesFrame.getOverflow();

        Frame frame = new Frame(logger, targetFrame,
            contentLocation,
            new RectangleSize(ds.getWidth(), ds.getHeight()),
            new RectangleSize(clientWidth, clientHeight),
            originalLocation, originalOverflow);

        getDriver().getFrameChain().push(frame);
    }

    public WebDriver frame(int index) {
        logger.verbose(String.format("EyesTargetLocator.frame(%d)", index));
        // Finding the target element so and reporting it using onWillSwitch.
        WebElement targetFrame = getFrameByIndex(index);
        logger.verbose("Done! Making preparations...");
        willSwitchToFrame(targetFrame);
        logger.verbose("Done! Switching to frame...");
        switchToFrame(targetFrame);
        logger.verbose("Done!");
        return getDriver();
    }


    public WebDriver frame(String nameOrId) {
        logger.verbose(String.format("EyesTargetLocator.frame('%s')",
            nameOrId));
        // Finding the target element so we can report it.
        // We use find elements(plural) to avoid exception when the element
        // is not found.
        logger.verbose("Getting frames by name...");
        List<WebElement> frames = getFramesByName(nameOrId);
        if (frames.size() == 0) {
            logger.verbose("No frames Found! Trying by id...");
            // If there are no frames by that name, we'll try the id
            frames = getDriver().findElementsById(nameOrId);
            if (frames.size() == 0) {
                // No such frame, bummer
                throw new NoSuchFrameException(String.format(
                    "No frame with name or id '%s' exists!", nameOrId));
            }
        }
        logger.verbose("Done! Making preparations...");
        willSwitchToFrame(frames.get(0));
        logger.verbose("Done! Switching to frame...");
        switchToFrame(nameOrId);
        logger.verbose("Done!");
        return getDriver();
    }


    public WebDriver frame(WebElement frameElement) {
        logger.verbose("EyesTargetLocator.frame(element)");
        logger.verbose("Making preparations...");
        willSwitchToFrame(frameElement);
        logger.verbose("Done! Switching to frame...");
        switchToFrame(frameElement);
        logger.verbose("Done!");
        return getDriver();
    }

    public WebDriver parentFrame() {
        logger.verbose("EyesWebTargetLocator.parentFrame()");
        if (getDriver().getFrameChain().size() != 0) {
            logger.verbose("Making preparations..");
            getDriver().getFrameChain().pop();
            logger.verbose("Done! Switching to parent frame..");
            switchToParent();
        }
        logger.verbose("Done!");
        return getDriver();
    }

    /**
     * Switches into every frame in the frame chain. This is used as way to
     * switch into nested frames (while considering scroll) in a single call.
     * @param frameChain The path to the frame to switch to.
     * @return The WebDriver with the switched context.
     */
    public WebDriver framesDoScroll(FrameChain frameChain) {
        logger.verbose("EyesWebTargetLocator.framesDoScroll(frameChain)");
        switchToDefault();
        for (Frame frame : frameChain) {
            logger.verbose("Scrolling by parent scroll position...");
            Location frameLocation = frame.getLocation();
            getScrollProvider().setPosition(frameLocation);
            logger.verbose("Done! Switching to frame...");
            switchToFrame(frame.getReference());
            logger.verbose("Done!");
        }

        logger.verbose("Done switching into nested frames!");
        return getDriver();
    }

    /**
     * Switches into every frame in the frame chain. This is used as way to
     * switch into nested frames (while considering scroll) in a single call.
     * @param frameChain The path to the frame to switch to.
     * @return The WebDriver with the switched context.
     */
    public WebDriver frames(FrameChain frameChain) {
        logger.verbose("EyesWebTargetLocator.frames(frameChain)");
        switchToDefault();
        for (Frame frame : frameChain) {
            switchToFrame(frame.getReference());
        }
        logger.verbose("Done switching into nested frames!");
        return getDriver();
    }

    /**
     * Switches into every frame in the list. This is used as way to
     * switch into nested frames in a single call.
     * @param framesPath The path to the frame to check. This is a list of
     *                   frame names/IDs (where each frame is nested in the
     *                   previous frame).
     * @return The WebDriver with the switched context.
     */
    public WebDriver frames(String[] framesPath) {
        logger.verbose("EyesWebTargetLocator.frames(framesPath)");
        for (String frameNameOrId : framesPath) {
            logger.verbose("Switching to frame...");
            switchToFrame(frameNameOrId);
            logger.verbose("Done!");
        }
        logger.verbose("Done switching into nested frames!");
        return getDriver();
    }

    public WebDriver defaultContent() {
        logger.verbose("EyesWebTargetLocator.defaultContent()");
        if (getDriver().getFrameChain().size() != 0) {
            logger.verbose("Making preparations...");
            getDriver().getFrameChain().clear();
            logger.verbose("Done! Switching to default content...");
            switchToDefault();
            logger.verbose("Done!");
        }
        return getDriver();
    }

    public WebElement activeElement() {
        logger.verbose("EyesWebTargetLocator.activeElement()");
        logger.verbose("Switching to element...");
        WebElement element = targetLocator.activeElement();
        if (!(element instanceof RemoteWebElement)) {
            throw new EyesException("Not a remote web element!");
        }
        EyesRemoteWebElement result = getDriver().getEyesElement(element);
        logger.verbose("Done!");
        return result;
    }

    public Alert alert() {
        logger.verbose("EyesWebTargetLocator.alert()");
        logger.verbose("Switching to alert...");
        Alert result = targetLocator.alert();
        logger.verbose("Done!");
        return result;
    }

    public WebDriver window(String nameOrHandle) {
        logger.verbose("EyesWebTargetLocator.window()");
        getDriver().getFrameChain().clear();
        logger.verbose("Done! Switching to window...");
        switchToWindow(nameOrHandle);
        logger.verbose("Done!");
        return getDriver();
    }

}
