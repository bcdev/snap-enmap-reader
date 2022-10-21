package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.VirtualDir;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandSelectDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;

class L1BSpectrumImageReader implements EnmapImageReader {

    private final GeoTiffImageReader vnirImageReader;
    private final GeoTiffImageReader swirImageReader;

    public L1BSpectrumImageReader(VirtualDir dataDir, EnmapMetadata meta, String vnirImageKey, String swirImageKey) throws IOException {
        Map<String, String> fileNameMap = meta.getFileNameMap();
        vnirImageReader = EnmapImageReader.createImageReader(dataDir, fileNameMap.get(vnirImageKey));
        swirImageReader = EnmapImageReader.createImageReader(dataDir, fileNameMap.get(swirImageKey));
    }

    @Override
    public Dimension getTileDimension() throws IOException {
        TIFFRenderedImage baseImage = vnirImageReader.getBaseImage();
        return new Dimension(baseImage.getTileWidth(), baseImage.getTileHeight());
    }

    @Override
    public int getNumImages() throws IOException {
        return getNumVnirImages() + getNumSwirImages();
    }

    public int getNumVnirImages() throws IOException {
        return vnirImageReader.getBaseImage().getSampleModel().getNumBands();
    }

    public int getNumSwirImages() throws IOException {
        return swirImageReader.getBaseImage().getSampleModel().getNumBands();
    }

    @Override
    public RenderedImage getImageAt(int index) throws IOException {
        int vnirImages = getNumVnirImages();
        int swirImages = getNumSwirImages();
        int maxImages = vnirImages + swirImages;
        if (index < vnirImages) {
            RenderedOp renderedOp = BandSelectDescriptor.create(vnirImageReader.getBaseImage(), new int[]{index}, null);
            return renderedOp;
        } else if (index < maxImages) {
            return BandSelectDescriptor.create(swirImageReader.getBaseImage(), new int[]{index - vnirImages}, null);
        } else {
            throw new IllegalArgumentException(String.format("Image index must be between 0 and %d", maxImages - 1));
        }
    }

    @Override
    public void close() {
        vnirImageReader.close();
        swirImageReader.close();
    }
}
