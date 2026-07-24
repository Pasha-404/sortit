package com.pavelkuzmin.sortit.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

class MediaDateExtractorTest {
    @TempDir
    Path tempDir;

    @Test
    void metadataFreePngDoesNotUseFileSystemDate() throws Exception {
        Path image = tempDir.resolve("plain.png");
        BufferedImage pixels = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(pixels, "png", image.toFile());

        assertNull(MediaDateExtractor.getBestDate(image.toFile()));
    }
}
