package com.applitools.eyes.appium;

import com.applitools.eyes.CutProvider;
import com.applitools.eyes.Logger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.ScaleProviderFactory;
import com.applitools.eyes.capture.EyesScreenshotFactory;
import com.applitools.eyes.capture.ImageProvider;
import com.applitools.eyes.debug.DebugScreenshotsProvider;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.selenium.capture.FullPageCaptureAlgorithm;
import java.awt.image.BufferedImage;

public class AppiumFullPageCaptureAlgorithm extends FullPageCaptureAlgorithm {

    public AppiumFullPageCaptureAlgorithm(Logger logger,
        AppiumScrollPositionProvider scrollProvider,
        ImageProvider imageProvider, DebugScreenshotsProvider debugScreenshotsProvider,
        ScaleProviderFactory scaleProviderFactory, CutProvider cutProvider,
        EyesScreenshotFactory screenshotFactory, int waitBeforeScreenshots) {

        super(logger, scrollProvider, scrollProvider, scrollProvider, imageProvider,
            debugScreenshotsProvider, scaleProviderFactory, cutProvider, screenshotFactory,
            waitBeforeScreenshots);
    }

    @Override
    protected void captureAndStitchTailParts(BufferedImage image, int stitchingOverlap,
        RectangleSize entireSize, RectangleSize initialPartSize) {

    }

}
