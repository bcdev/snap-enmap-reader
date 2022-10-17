package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

interface SpectralImageReader {
    static SpectralImageReader create(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        String processingLevel = meta.getProcessingLevel();
        switch (EnmapMetadata.PROCESSING_LEVEL.valueOf(processingLevel)) {
            case L1B:
                return new L1BSpectralImageReader(dataDir, meta);
            case L1C:
                return new L1CSpectralImageReader(dataDir, meta);
            case L2A:
                return new L2ASpectralImageReader(dataDir, meta);
            default:
                throw new IOException(String.format("Unknown product level '%s'", processingLevel));
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
