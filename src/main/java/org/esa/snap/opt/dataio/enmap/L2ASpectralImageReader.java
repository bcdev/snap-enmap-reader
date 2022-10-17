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

import static org.esa.snap.opt.dataio.enmap.EnmapFileUtils.SPECTRAL_IMAGE_KEY;

public class L2ASpectralImageReader implements SpectralImageReader{

    private final GeoTiffImageReader spectralImageReader;

    public L2ASpectralImageReader(VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        Map<String, String> fileNameMap = meta.getFileNameMap();
        spectralImageReader = SpectralImageReader.createImageReader(dataDir, fileNameMap.get(SPECTRAL_IMAGE_KEY));
    }

    @Override
    public Dimension getTileDimension() throws IOException {
        TIFFRenderedImage baseImage = spectralImageReader.getBaseImage();
        return new Dimension(baseImage.getTileWidth(), baseImage.getTileHeight());
    }

    @Override
    public RenderedImage getImageAt(int index) throws IOException {
        return BandSelectDescriptor.create(spectralImageReader.getBaseImage(), new int[]{index}, null);
    }

    @Override
    public void close() {
        spectralImageReader.close();
    }
}
