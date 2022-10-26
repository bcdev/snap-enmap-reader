package org.esa.snap.opt.enmap;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

import static org.esa.snap.opt.enmap.EnmapFileUtils.*;

interface EnmapImageReader {
    static EnmapImageReader createSpectralReader(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        // todo - In Java 9 this can be replaced by createEnmapImageReader() (see below)
        // todo - but in Java 8 private static is not allowed yet.
        switch (meta.getProcessingLevel()) {
            case L1B:
                return new L1BSpectrumImageReader(dataDir, meta, SPECTRAL_IMAGE_VNIR_KEY, SPECTRAL_IMAGE_SWIR_KEY);
            case L1C:
            case L2A:
                return new GenericImageReader(dataDir, meta, SPECTRAL_IMAGE_KEY);
            default:
                throw new IOException(String.format("Unknown product level '%s'", meta.getProcessingLevel()));
        }
    }

    static EnmapImageReader createPixelMaskReader(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        // todo - In Java 9 this can be replaced by createEnmapImageReader() (see below)
        // todo - but in Java 8 private static is not allowed yet.
        switch (meta.getProcessingLevel()) {
            case L1B:
                return new L1BSpectrumImageReader(dataDir, meta, QUALITY_PIXELMASK_VNIR_KEY, QUALITY_PIXELMASK_SWIR_KEY);
            case L1C:
            case L2A:
                return new GenericImageReader(dataDir, meta, QUALITY_PIXELMASK_KEY);
            default:
                throw new IOException(String.format("Unknown product level '%s'", meta.getProcessingLevel()));
        }
    }

    static EnmapImageReader createQualityReader(VirtualDir dataDir, EnmapMetadata meta, String qualityKey) throws IOException {
        return new GenericImageReader(dataDir, meta, qualityKey);
    }

    // todo - use this with Java 9
//    private static EnmapImageReader createEnmapImageReader(VirtualDir dataDir, EnmapMetadata meta, String spectralImageVnirKey, String spectralImageSwirKey, String spectralImageKey) throws IOException {
//        switch (meta.getProcessingLevel()) {
//            case L1B:
//                return new L1BSpectrumImageReader(dataDir, meta, spectralImageVnirKey, spectralImageSwirKey);
//            case L1C:
//            case L2A:
//                return new GenericImageReader(dataDir, meta, spectralImageKey);
//            default:
//                throw new IOException(String.format("Unknown product level '%s'", meta.getProcessingLevel()));
//        }
//    }

    static GeoTiffImageReader createImageReader(VirtualDir dataDir, String fileName) throws IOException {
        try {
            return new GeoTiffImageReader(getInputStream(dataDir, fileName), () -> {
            });
        } catch (IllegalStateException ise) {
            throw new IOException("Could not create spectral data reader.", ise);
        }
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
