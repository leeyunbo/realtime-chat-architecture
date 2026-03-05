package com.bok.chat.api.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageResizer {

    private static final int MAX_THUMBNAIL_SIZE = 200;

    public byte[] resize(byte[] imageData, String contentType) throws IOException {
        int orientation = readExifOrientation(imageData);

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageData));
        if (original == null) {
            throw new IOException("Failed to read image");
        }

        BufferedImage oriented = applyOrientation(original, orientation);

        int width = oriented.getWidth();
        int height = oriented.getHeight();

        if (width <= MAX_THUMBNAIL_SIZE && height <= MAX_THUMBNAIL_SIZE) {
            if (orientation == 1) {
                return imageData;
            }
            return writeImage(oriented, contentType);
        }

        BufferedImage thumbnail = createThumbnail(oriented, width, height);
        return writeImage(thumbnail, contentType);
    }

    private byte[] writeImage(BufferedImage image, String contentType) throws IOException {
        String formatName = ImageFormat.formatNameOf(contentType);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, out);
        return out.toByteArray();
    }

    private int readExifOrientation(byte[] imageData) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageData));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            // EXIF 읽기 실패 시 기본값
        }
        return 1;
    }

    private BufferedImage applyOrientation(BufferedImage image, int orientation) {
        if (orientation == 1) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        AffineTransform transform = new AffineTransform();

        switch (orientation) {
            case 2 -> { // Flip horizontal
                transform.scale(-1, 1);
                transform.translate(-width, 0);
            }
            case 3 -> { // Rotate 180
                transform.rotate(Math.PI, width / 2.0, height / 2.0);
            }
            case 4 -> { // Flip vertical
                transform.scale(1, -1);
                transform.translate(0, -height);
            }
            case 5 -> { // Rotate 90 CW + flip horizontal
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
            }
            case 6 -> { // Rotate 90 CW
                transform.rotate(Math.PI / 2);
                transform.translate(0, -height);
            }
            case 7 -> { // Rotate 90 CCW + flip horizontal
                transform.rotate(-Math.PI / 2);
                transform.scale(1, -1);
                transform.translate(-width, 0);
            }
            case 8 -> { // Rotate 90 CCW
                transform.rotate(-Math.PI / 2);
                transform.translate(-width, 0);
            }
            default -> { return image; }
        }

        boolean swapDimensions = orientation >= 5;
        int newWidth = swapDimensions ? height : width;
        int newHeight = swapDimensions ? width : height;

        int imageType = image.getType() != 0 ? image.getType() : BufferedImage.TYPE_INT_ARGB;
        BufferedImage result = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, transform, null);
        g2d.dispose();

        return result;
    }

    private BufferedImage createThumbnail(BufferedImage original, int originalWidth, int originalHeight) {
        double scale = Math.min(
                (double) MAX_THUMBNAIL_SIZE / originalWidth,
                (double) MAX_THUMBNAIL_SIZE / originalHeight
        );

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        int imageType = original.getType() != 0 ? original.getType() : BufferedImage.TYPE_INT_ARGB;
        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return thumbnail;
    }
}
