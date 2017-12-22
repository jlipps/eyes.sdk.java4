package com.applitools.eyes.selenium.capture;

import com.applitools.eyes.*;
import com.applitools.eyes.capture.EyesScreenshotFactory;
import com.applitools.eyes.capture.ImageProvider;
import com.applitools.eyes.debug.DebugScreenshotsProvider;
import com.applitools.eyes.CutProvider;
import com.applitools.eyes.positioning.PositionMemento;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.positioning.ScrollingPositionProvider;
import com.applitools.eyes.selenium.EyesSeleniumUtils;
import com.applitools.eyes.selenium.exceptions.EyesDriverOperationException;
import com.applitools.eyes.selenium.positioning.NullRegionPositionCompensation;
import com.applitools.eyes.selenium.positioning.RegionPositionCompensation;
import com.applitools.eyes.selenium.positioning.ScrollPositionProvider;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import com.applitools.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import javax.swing.text.Position;
import sun.security.ssl.Debug;

public class FullPageCaptureAlgorithm {

    private static final int MIN_SCREENSHOT_PART_HEIGHT = 10;

    protected Logger logger;

    private final PositionProvider originProvider;
    protected final ImageProvider imageProvider;
    protected final DebugScreenshotsProvider debugScreenshotsProvider;
    private final ScaleProviderFactory scaleProviderFactory;
    private final EyesScreenshotFactory screenshotFactory;
    protected final int waitBeforeScreenshots;

    private PositionMemento originalPosition;
    private ScaleProvider scaleProvider;
    private CutProvider cutProvider;
    private Region regionInScreenshot;
    private double pixelRatio;
    private BufferedImage stitchedImage;
    protected Location currentPosition;

    protected final PositionProvider positionProvider;
    protected final ScrollingPositionProvider scrollProvider;

    public FullPageCaptureAlgorithm(Logger logger, PositionProvider originProvider,
        PositionProvider positionProvider,
        ScrollingPositionProvider scrollProvider,
        ImageProvider imageProvider, DebugScreenshotsProvider debugScreenshotsProvider,
        ScaleProviderFactory scaleProviderFactory, CutProvider cutProvider,
        EyesScreenshotFactory screenshotFactory, int waitBeforeScreenshots) {
        ArgumentGuard.notNull(logger, "logger");
        this.logger = logger;
        this.originProvider = originProvider;
        this.positionProvider = positionProvider;
        this.scrollProvider = scrollProvider;
        this.imageProvider = imageProvider;
        this.debugScreenshotsProvider = debugScreenshotsProvider;
        this.scaleProviderFactory = scaleProviderFactory;
        this.cutProvider = cutProvider;
        this.screenshotFactory = screenshotFactory;
        this.waitBeforeScreenshots = waitBeforeScreenshots;
        this.pixelRatio = 1.0;
        this.originalPosition = null;
        this.scaleProvider = null;
        this.regionInScreenshot = null;
        this.stitchedImage = null;
        this.currentPosition = null;
    }

    private void saveDebugScreenshotPart(BufferedImage image,
        Region region, String name) {
        String suffix =
            "part-" + name + "-" + region.getLeft() + "_" + region.getTop() + "_" + region
                .getWidth() + "x"
                + region.getHeight();
        debugScreenshotsProvider.save(image, suffix);
    }

    protected void moveToTopLeft() {
        logger.verbose("Moving to the top left of the screen");
        int setPositionRetries = 3;
        do {
            originProvider.setPosition(new Location(0, 0));
            // Give the scroll time to stabilize
            GeneralUtils.sleep(waitBeforeScreenshots);
            currentPosition = originProvider.getCurrentPosition();
        } while (currentPosition.getX() != 0
            && currentPosition.getY() != 0
            && (--setPositionRetries > 0));

        if (currentPosition.getX() != 0 || currentPosition.getY() != 0) {
            originProvider.restoreState(originalPosition);
            throw new EyesException("Couldn't set position to the top/left corner!");
        }
    }

    private BufferedImage getTopLeftScreenshot() {
        moveToTopLeft();
        logger.verbose("Getting top/left image...");
        BufferedImage image = imageProvider.getImage();
        debugScreenshotsProvider.save(image, "original");

        // FIXME - scaling should be refactored
        scaleProvider = scaleProviderFactory.getScaleProvider(image.getWidth());
        // Notice that we want to cut/crop an image before we scale it, we need to change
        pixelRatio = 1 / scaleProvider.getScaleRatio();

        // FIXME - cropping should be overlaid, so a single cut provider will only handle a single part of the image.
        cutProvider = cutProvider.scale(pixelRatio);
        if (!(cutProvider instanceof NullCutProvider)) {
            image = cutProvider.cut(image);
            debugScreenshotsProvider.save(image, "original-cut");
        }

        return image;
    }

    private BufferedImage cropToRegion(BufferedImage image, Region region,
        RegionPositionCompensation regionPositionCompensation) {

        setRegionInScreenshot(image, region, regionPositionCompensation);

        if (!regionInScreenshot.isEmpty()) {
            image = ImageUtils.getImagePart(image, regionInScreenshot);
            saveDebugScreenshotPart(image, region, "cropped");
        }

        if (pixelRatio != 1.0) {
            image = ImageUtils.scaleImage(image, scaleProvider);
            debugScreenshotsProvider.save(image, "scaled");
        }

        return image;
    }

    private RectangleSize getEntireSize(BufferedImage image, boolean checkingAnElement) {
        RectangleSize entireSize;
        if (!checkingAnElement) {
            try {
                entireSize = scrollProvider.getEntireSize();
                logger.verbose("Entire size of region context: " + entireSize);
            } catch (EyesDriverOperationException e) {
                logger.log("WARNING: Failed to extract entire size of region context" + e.getMessage());
                logger.log("Using image size instead: " + image.getWidth() + "x" + image.getHeight());
                entireSize = new RectangleSize(image.getWidth(), image.getHeight());
            }
        } else {
            entireSize = positionProvider.getEntireSize();
        }
        return entireSize;
    }

    protected void setRegionInScreenshot (BufferedImage image, Region region,
        RegionPositionCompensation regionPositionCompensation) {

        logger.verbose("Creating screenshot object...");
        // We need the screenshot to be able to convert the region to screenshot coordinates.
        EyesScreenshot screenshot = screenshotFactory.makeScreenshot(image);
        logger.verbose("Getting region in screenshot...");
        regionInScreenshot = getRegionInScreenshot(region, image, pixelRatio, screenshot,
            regionPositionCompensation);

        // if it didn't work the first time, just try again!??
        if (!regionInScreenshot.getSize().equals(region.getSize())) {
            // TODO - ITAI
            regionInScreenshot = getRegionInScreenshot(region, image, pixelRatio, screenshot,
                regionPositionCompensation);
        }
    }

    protected BufferedImage cropPartToRegion(BufferedImage partImage, Region partRegion) {

        // FIXME - cropping should be overlaid (see previous comment re cropping)
        if (!(cutProvider instanceof NullCutProvider)) {
            logger.verbose("cutting...");
            partImage = cutProvider.cut(partImage);
            debugScreenshotsProvider.save(partImage,
                "original-scrolled-cut-" + positionProvider.getCurrentPosition()
                    .toStringForFilename());
        }

        if (!regionInScreenshot.isEmpty()) {
            logger.verbose("cropping...");
            partImage = ImageUtils.getImagePart(partImage, regionInScreenshot);
            saveDebugScreenshotPart(partImage, partRegion,
                "original-scrolled-"
                    + positionProvider.getCurrentPosition().toStringForFilename());
        }

        if (pixelRatio != 1.0) {
            logger.verbose("scaling...");
            // FIXME - scaling should be refactored
            partImage = ImageUtils.scaleImage(partImage, scaleProvider);
            saveDebugScreenshotPart(partImage, partRegion,
                "original-scrolled-" + positionProvider.getCurrentPosition()
                    .toStringForFilename() + "-scaled-");
        }
        return partImage;
    }

    protected void cleanupStitch(PositionMemento originalStitchedState,
        Location lastSuccessfulLocation,
        RectangleSize lastSuccessfulPartSize, RectangleSize entireSize) {

        logger.verbose("Stitching done!");
        positionProvider.restoreState(originalStitchedState);
        originProvider.restoreState(originalPosition);

        // If the actual image size is smaller than the extracted size, we crop the image.
        int actualImageWidth = lastSuccessfulLocation.getX() + lastSuccessfulPartSize.getWidth();
        int actualImageHeight = lastSuccessfulLocation.getY() + lastSuccessfulPartSize.getHeight();
        logger.verbose("Extracted entire size: " + entireSize);
        logger.verbose("Actual stitched size: " + actualImageWidth + "x" + actualImageHeight);

        if (actualImageWidth < stitchedImage.getWidth() || actualImageHeight < stitchedImage
            .getHeight()) {
            logger.verbose("Trimming unnecessary margins..");
            stitchedImage = ImageUtils.getImagePart(stitchedImage,
                new Region(0, 0,
                    Math.min(actualImageWidth, stitchedImage.getWidth()),
                    Math.min(actualImageHeight, stitchedImage.getHeight())));
            logger.verbose("Done!");
        }

        debugScreenshotsProvider.save(stitchedImage, "stitched");
    }

    private void captureAndStitchPart(Region partRegion) {
        logger.verbose(String.format("Taking screenshot for %s", partRegion));
        // Set the position to the part's top/left.
        positionProvider.setPosition(partRegion.getLocation());
        // Giving it time to stabilize.
        GeneralUtils.sleep(waitBeforeScreenshots);
        // Screen size may cause the scroll to only reach part of the way.
        currentPosition = positionProvider.getCurrentPosition();
        logger.verbose(String.format("Set position to %s", currentPosition));

        // Actually taking the screenshot.
        logger.verbose("Getting image...");
        BufferedImage partImage = imageProvider.getImage();
        debugScreenshotsProvider.save(partImage,
            "original-scrolled-" + positionProvider.getCurrentPosition().toStringForFilename());

        partImage = cropPartToRegion(partImage, partRegion);
        stitchPartIntoContainer(partImage);
    }

    protected void stitchPartIntoContainer(BufferedImage partImage) {
        // Stitching the current part.
        logger.verbose("Stitching part into the image container...");
        stitchedImage.getRaster()
            .setRect(currentPosition.getX(), currentPosition.getY(), partImage.getData());
        logger.verbose("Done!");
    }

    protected void captureAndStitchTailParts(BufferedImage image, int stitchingOverlap,
        RectangleSize entireSize, RectangleSize initialPartSize) {

        // The screenshot part is a bit smaller than the image size,
        // in order to eliminate duplicate bottom scroll bars, as well as fixed
        // position footers.
        RectangleSize partImageSize =
            new RectangleSize(image.getWidth(),
                Math.max(image.getHeight() - stitchingOverlap, MIN_SCREENSHOT_PART_HEIGHT));

        logger.verbose(
            String.format("Total size: %s, image part size: %s", entireSize, partImageSize));

        // Getting the list of sub-regions composing the whole region (we'll
        // take a screenshot for each one).
        Region entirePage = new Region(Location.ZERO, entireSize);
        Iterable<Region> imageParts = entirePage.getSubRegions(partImageSize);

        // These will be used for storing the actual stitched size (it is
        // sometimes less than the size extracted via "getEntireSize").
        Location lastSuccessfulLocation = new Location(0, 0);
        RectangleSize lastSuccessfulPartSize = new RectangleSize(initialPartSize.getWidth(),
            initialPartSize.getHeight());

        PositionMemento originalStitchedState = positionProvider.getState();

        // Take screenshot and stitch for each screenshot part.
        logger.verbose("Getting the rest of the image parts...");
        BufferedImage partImage = null;
        for (Region partRegion : imageParts) {
            // Skipping screenshot for 0,0 (already taken)
            if (partRegion.getLeft() == 0 && partRegion.getTop() == 0) {
                continue;
            }
            captureAndStitchPart(partRegion);
            lastSuccessfulLocation = currentPosition;
        }

        if (partImage != null) {
            lastSuccessfulPartSize = new RectangleSize(partImage.getWidth(), partImage.getHeight());
        }

        cleanupStitch(originalStitchedState, lastSuccessfulLocation, lastSuccessfulPartSize,
            entireSize);
    }

    /**
     * Returns a stitching of a region.
     *
     * @param region The region to stitch. If {@code Region.EMPTY}, the entire image will be stitched.
     * @param stitchingOverlap The width of the overlapping parts when stitching an image.
     * @param regionPositionCompensation A strategy for compensating region positions for some browsers.
     * @return An image which represents the stitched region.
     */
    public BufferedImage getStitchedRegion(Region region, int stitchingOverlap,
        RegionPositionCompensation regionPositionCompensation) {
        logger.verbose("getStitchedRegion()");

        ArgumentGuard.notNull(region, "region");

        logger.verbose(String.format(
            "getStitchedRegion: originProvider: %s ; positionProvider: %s ; cutProvider: %s",
            originProvider.getClass(), positionProvider.getClass(), cutProvider.getClass()));

        logger.verbose(String.format("Region to check: %s", region));

        // Saving the original position (in case we were already in the outermost frame).
        originalPosition = originProvider.getState();

        // first, scroll to the origin and get the top left screenshot
        BufferedImage image = getTopLeftScreenshot();

        // now crop the screenshot based on the provided region
        image = cropToRegion(image, region, regionPositionCompensation);

        // get the entire size of the region context, falling back to image size

        boolean checkingAnElement = !region.isEmpty();
        RectangleSize entireSize = getEntireSize(image, checkingAnElement);

        // If the image is already the same as or bigger than the entire size, we're done!
        // Notice that this might still happen even if we used
        // "getImagePart", since "entirePageSize" might be that of a frame.
        if (image.getWidth() >= entireSize.getWidth() && image.getHeight() >= entireSize
            .getHeight()) {
            originProvider.restoreState(originalPosition);

            return image;
        }

        // Otherwise, make a big image to stitch smaller parts into
        logger.verbose("Creating stitchedImage container. Size: " + entireSize);
        //Notice stitchedImage uses the same type of image as the screenshots.
        stitchedImage = new BufferedImage(
            entireSize.getWidth(), entireSize.getHeight(), image.getType());
        logger.verbose("Done!");

        // First of all we want to stitch the screenshot we already captured at (0, 0)
        logger.verbose("Adding initial screenshot..");
        Raster initialPart = image.getData();
        RectangleSize initialPartSize = new RectangleSize(initialPart.getWidth(),
            initialPart.getHeight());
        logger.verbose(String.format("Initial part:(0,0)[%d x %d]",
            initialPart.getWidth(), initialPart.getHeight()));
        stitchedImage.getRaster().setRect(0, 0, initialPart);
        logger.verbose("Done!");

        captureAndStitchTailParts(image, stitchingOverlap, entireSize, initialPartSize);

        return stitchedImage;
    }

    private Region getRegionInScreenshot(Region region, BufferedImage image, double pixelRatio,
        EyesScreenshot screenshot, RegionPositionCompensation regionPositionCompensation) {
        // Region regionInScreenshot = screenshot.convertRegionLocation(regionProvider.getRegion(), regionProvider.getCoordinatesType(), CoordinatesType.SCREENSHOT_AS_IS);
        Region regionInScreenshot = screenshot
            .getIntersectedRegion(region, CoordinatesType.SCREENSHOT_AS_IS);

        logger.verbose("Done! Region in screenshot: " + regionInScreenshot);
        regionInScreenshot = regionInScreenshot.scale(pixelRatio);
        logger.verbose("Scaled region: " + regionInScreenshot);

        if (regionPositionCompensation == null) {
            regionPositionCompensation = new NullRegionPositionCompensation();
        }

        regionInScreenshot = regionPositionCompensation
            .compensateRegionPosition(regionInScreenshot, pixelRatio);

        // Handling a specific case where the region is actually larger than
        // the screenshot (e.g., when body width/height are set to 100%, and
        // an internal div is set to value which is larger than the viewport).
        regionInScreenshot.intersect(new Region(0, 0, image.getWidth(), image.getHeight()));
        logger.verbose("Region after intersect: " + regionInScreenshot);
        return regionInScreenshot;
    }
}
