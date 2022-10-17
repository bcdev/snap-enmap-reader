package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.VirtualDir;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import javax.media.jai.operator.BandSelectDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;

import static org.esa.snap.opt.dataio.enmap.EnmapFileUtils.*;

public class L1BSpectralImageReader implements SpectralImageReader {

    private final GeoTiffImageReader vnirImageReader;
    private final GeoTiffImageReader swirImageReader;

    public L1BSpectralImageReader(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        Map<String, String> fileNameMap = meta.getFileNameMap();
        vnirImageReader = SpectralImageReader.createImageReader(dataDir, fileNameMap.get(SPECTRAL_IMAGE_VNIR_KEY));
        swirImageReader = SpectralImageReader.createImageReader(dataDir, fileNameMap.get(SPECTRAL_IMAGE_SWIR_KEY));
    }

    @Override
    public Dimension getTileDimension() throws IOException {
        TIFFRenderedImage baseImage = vnirImageReader.getBaseImage();
        return new Dimension(baseImage.getTileWidth(), baseImage.getTileHeight());
    }

    @Override
    public RenderedImage getImageAt(int index) throws IOException {
        int vnirImages = vnirImageReader.getBaseImage().getSampleModel().getNumBands();
        int swirImages = swirImageReader.getBaseImage().getSampleModel().getNumBands();
        int maxImages = vnirImages + swirImages;
        if (index < vnirImages) {
            return BandSelectDescriptor.create(vnirImageReader.getBaseImage(), new int[]{index}, null);
        } else if (index < maxImages) {
            return BandSelectDescriptor.create(swirImageReader.getBaseImage(), new int[]{index - vnirImages}, null);
        } else {
            throw new IllegalArgumentException(String.format("Spectral index must be between 0 and %d", maxImages - 1));
        }
    }

    @Override
    public void close() {
        vnirImageReader.close();
        swirImageReader.close();
    }
}
