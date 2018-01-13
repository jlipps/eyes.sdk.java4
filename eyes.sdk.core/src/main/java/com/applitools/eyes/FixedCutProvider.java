package com.applitools.eyes;

import com.applitools.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Cut provider based on fixed cut values, run AFTER scaling (so coordinates should be normalized).
 */
@SuppressWarnings("WeakerAccess")
public class FixedCutProvider implements CutProvider {

    private final int header;
    private final int footer;
    private final int left;
    private final int right;

    /**
     *
     * @param header The header to cut in pixels.
     * @param footer The footer to cut in pixels.
     * @param left The left to cut in pixels.
     * @param right The right to cut in pixels.
     */
    public FixedCutProvider(int header, int footer, int left, int right) {
        this.header = header;
        this.footer = footer;
        this.left = left;
        this.right = right;
    }

    public BufferedImage cut(BufferedImage image) {
        if (header > 0) {
            image = ImageUtils.cropImage(image,
                    new Region(0, header, image.getWidth(),
                            image.getHeight() - header));
        }

        if (footer > 0) {
            image = ImageUtils.cropImage(image,
                    new Region(0, 0,
                            image.getWidth(), image.getHeight() - footer));
        }

        if (left > 0) {
            image = ImageUtils.cropImage(image,
                    new Region(left, 0, image.getWidth() - left,
                            image.getHeight()));
        }

        if (right > 0) {
            image = ImageUtils.cropImage(image,
                    new Region(0, 0, image.getWidth() - right,
                            image.getHeight()));
        }

        return image;
    }

    public CutProvider scale(double scaleRatio) {
        int scaledHeader = (int) Math.round(header * scaleRatio);
        int scaledFooter = (int) Math.round(footer * scaleRatio);
        int scaledLeft = (int) Math.round(left * scaleRatio);
        int scaledRight = (int) Math.round(right * scaleRatio);

        return new FixedCutProvider(scaledHeader, scaledFooter, scaledLeft, scaledRight);
    }
}
