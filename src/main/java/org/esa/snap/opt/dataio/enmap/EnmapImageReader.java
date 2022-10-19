package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

import static org.esa.snap.opt.dataio.enmap.EnmapFileUtils.*;

interface EnmapImageReader {
    static EnmapImageReader createSpectralReader(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        return createEnmapImageReader(dataDir, meta, SPECTRAL_IMAGE_VNIR_KEY, SPECTRAL_IMAGE_SWIR_KEY, SPECTRAL_IMAGE_KEY);
    }

    static EnmapImageReader createPixelMaskReader(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        return createEnmapImageReader(dataDir, meta, QUALITY_PIXELMASK_VNIR_KEY, QUALITY_PIXELMASK_SWIR_KEY, QUALITY_PIXELMASK_KEY);
    }

    static EnmapImageReader createEnmapImageReader(VirtualDir dataDir, EnmapMetadata meta, String spectralImageVnirKey, String spectralImageSwirKey, String spectralImageKey) throws IOException {
        switch (meta.getProcessingLevel()) {
            case L1B:
                return new L1BImageReader(dataDir, meta, spectralImageVnirKey, spectralImageSwirKey);
            case L1C:
            case L2A:
                return new OrthoImageReader(dataDir, meta, spectralImageKey);
            default:
                throw new IOException(String.format("Unknown product level '%s'", meta.getProcessingLevel()));
        }
    }

    static GeoTiffImageReader createImageReader(VirtualDir dataDir, String fileName) throws IOException {
        final GeoTiffImageReader imageReader;
        try {
            imageReader = new GeoTiffImageReader(dataDir.getInputStream(fileName), () -> {
            });
        } catch (IllegalStateException ise) {
            throw new IOException("Could not create spectral data reader.", ise);
        }
        return imageReader;
    }

    /**
     * returns the dimension of the image tiles
     *
     * @return the tile dimension
     * @throws IOException in case the information could not be retrieved from the source
     */
    Dimension getTileDimension() throws IOException;

    /**
     * returns the number of images provided by this reader
     *
     * @return the number of images
     * @throws IOException in case the information could not be retrieved from the source
     */
    int getNumImages() throws IOException;
    /**
     * returns the spectral image at the specified index (zero based)
     *
     * @param index the spectral index
     * @return the image at the given spectral index
     * @throws IOException              in case the information could not be retrieved from the source
     * @throws IllegalArgumentException in case the index is less than zero or higher than the maximum number of images minus one
     */
    RenderedImage getImageAt(int index) throws IOException;

    /**
     * Closes any open resource
     */
    void close();
}
