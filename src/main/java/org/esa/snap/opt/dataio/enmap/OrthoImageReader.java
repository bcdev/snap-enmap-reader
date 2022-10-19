package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.VirtualDir;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;

import javax.media.jai.operator.BandSelectDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;

public class OrthoImageReader implements EnmapImageReader {

    private final GeoTiffImageReader tiffImageReader;

    public OrthoImageReader(VirtualDir dataDir, EnmapMetadata meta, String imageKey) throws IOException {
        Map<String, String> fileNameMap = meta.getFileNameMap();
        tiffImageReader = EnmapImageReader.createImageReader(dataDir, fileNameMap.get(imageKey));
    }

    @Override
    public Dimension getTileDimension() throws IOException {
        TIFFRenderedImage baseImage = tiffImageReader.getBaseImage();
        return new Dimension(baseImage.getTileWidth(), baseImage.getTileHeight());
    }

    @Override
    public int getNumImages() throws IOException {
        return tiffImageReader.getBaseImage().getSampleModel().getNumBands();
    }

    @Override
    public RenderedImage getImageAt(int index) throws IOException {
        return BandSelectDescriptor.create(tiffImageReader.getBaseImage(), new int[]{index}, null);
    }

    @Override
    public void close() {
        tiffImageReader.close();
    }
}
