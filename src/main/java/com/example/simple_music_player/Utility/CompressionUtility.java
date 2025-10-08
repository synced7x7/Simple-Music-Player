package com.example.simple_music_player.Utility;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class CompressionUtility {

    public static byte[] resizeAndCompress(Image image, int maxWidth, int maxHeight, float quality) {
        if (image == null) return null;
        try {
            // Convert JavaFX Image to BufferedImage
            BufferedImage original = SwingFXUtils.fromFXImage(image, null);

            // Compute new size while preserving aspect ratio
            double originalWidth = original.getWidth();
            double originalHeight = original.getHeight();
            double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            // Resize
            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
            g.dispose();

            // Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
            javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            jpgWriter.setOutput(ios);

            javax.imageio.ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(quality);

            jpgWriter.write(null, new javax.imageio.IIOImage(resized, null, null), jpgWriteParam);

            ios.close();
            jpgWriter.dispose();

            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

   /* For JPEG compression quality (0.0f to 1.0f):
        Good / High quality: 0.8f – 1.0f
        Medium / Balanced: 0.5f – 0.7f
        Low / Bad quality: 0.1f – 0.4f
    Anything below ~0.5 will noticeably degrade image clarity.*/
}
