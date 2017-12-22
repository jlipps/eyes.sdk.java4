package com.applitools.eyes.appium;

import com.applitools.eyes.CutProvider;
import com.applitools.eyes.Location;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.Region;
import com.applitools.eyes.ScaleProviderFactory;
import com.applitools.eyes.capture.EyesScreenshotFactory;
import com.applitools.eyes.capture.ImageProvider;
import com.applitools.eyes.debug.DebugScreenshotsProvider;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.selenium.capture.FullPageCaptureAlgorithm;
import com.applitools.eyes.selenium.positioning.NullRegionPositionCompensation;
import com.applitools.eyes.selenium.positioning.RegionPositionCompensation;
import com.applitools.utils.GeneralUtils;
import java.awt.image.BufferedImage;

public class AppiumFullPageCaptureAlgorithm extends FullPageCaptureAlgorithm {

    public AppiumFullPageCaptureAlgorithm(Logger logger,
        AppiumScrollPositionProvider scrollProvider,
        ImageProvider imageProvider, DebugScreenshotsProvider debugScreenshotsProvider,
        ScaleProviderFactory scaleProviderFactory, CutProvider cutProvider,
        EyesScreenshotFactory screenshotFactory, int waitBeforeScreenshots) {

        // ensure that all the scroll/position providers used by the superclass are the same object;
        // getting the current position for appium is very expensive!
        super(logger, scrollProvider, scrollProvider, scrollProvider, imageProvider,
            debugScreenshotsProvider, scaleProviderFactory, cutProvider, screenshotFactory,
            waitBeforeScreenshots);
    }

    private RectangleSize captureAndStitchCurrentPart(Region partRegion, Region scrollViewRegion) {

        logger.verbose("Taking screenshot for current scroll location");
        GeneralUtils.sleep(waitBeforeScreenshots);
        BufferedImage partImage = imageProvider.getImage();
        debugScreenshotsProvider.save(partImage,
            "original-scrolled=" + scrollProvider.getCurrentPosition().toStringForFilename());
        
        // before we take new screenshots, we have to reset the region in the screenshot we care
        // about, since from now on we just want the scroll view, not the entire view
        setRegionInScreenshot(partImage, scrollViewRegion, new NullRegionPositionCompensation());

        partImage = cropPartToRegion(partImage, partRegion);

        stitchPartIntoContainer(partImage);
        return new RectangleSize(partImage.getWidth(), partImage.getHeight());
    }

    @Override
    protected void captureAndStitchTailParts(BufferedImage image, int stitchingOverlap,
        RectangleSize entireSize, RectangleSize initialPartSize) {

        logger.verbose("Capturing all the tail parts for an Appium screen");

        Location lastSuccessfulLocation;
        RectangleSize lastSuccessfulPartSize = new RectangleSize(initialPartSize.getWidth(), initialPartSize.getHeight());
        PositionMemento originalStitchedState = scrollProvider.getState();
        Region scrollViewRegion = ((AppiumScrollPositionProvider) scrollProvider).getScrollableViewRegion();

        do {
            lastSuccessfulLocation = currentPosition;
            logger.verbose("Scrolling down to get next part");
            currentPosition = ((AppiumScrollPositionProvider) scrollProvider).scrollDown();
            logger.verbose("After scroll the position ended up at " + currentPosition);
            if (currentPosition.getX() == lastSuccessfulLocation.getX() && currentPosition.getY() == lastSuccessfulLocation.getY()) {
                logger.verbose("Scroll had no effect, breaking the scroll loop");
                break;
            }
            Region scrolledRegion = new Region(currentPosition.getX(), currentPosition.getY(), initialPartSize.getWidth(),
                initialPartSize.getHeight());
            logger.verbose("The region to capture will be " + scrolledRegion);
            lastSuccessfulPartSize = captureAndStitchCurrentPart(scrolledRegion, scrollViewRegion);
        }
        while (true);

        cleanupStitch(originalStitchedState, lastSuccessfulLocation, lastSuccessfulPartSize, entireSize);

    }

}
