package com.bok.chat.api.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageResizer {

    private static final int MAX_THUMBNAIL_SIZE = 200;

    public byte[] resize(byte[] imageData, String contentType) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageData));
        if (original == null) {
            throw new IOException("Failed to read image");
        }

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= MAX_THUMBNAIL_SIZE && originalHeight <= MAX_THUMBNAIL_SIZE) {
            return imageData;
        }

        BufferedImage thumbnail = createThumbnail(original, originalWidth, originalHeight);

        String formatName = ImageFormat.formatNameOf(contentType);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, formatName, out);
        return out.toByteArray();
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
