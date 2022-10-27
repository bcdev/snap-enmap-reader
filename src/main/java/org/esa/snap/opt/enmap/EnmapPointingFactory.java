package org.esa.snap.opt.enmap;

import org.esa.snap.core.datamodel.*;

import static org.esa.snap.opt.enmap.EnmapProductReader.*;

public class EnmapPointingFactory implements PointingFactory {

    private static final String[] SUPPORTED_TYPES = {"ENMAP_L1B"};

    @Override
    public String[] getSupportedProductTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Pointing createPointing(RasterDataNode raster) {
        final Product product = raster.getProduct();
        return new TiePointGridPointing(raster.getGeoCoding(),
                product.getTiePointGrid(SUN_ZENITH_TPG_NAME),
                product.getTiePointGrid(SUN_AZIMUTH_TPG_NAME),
                product.getTiePointGrid(SCENE_ZENITH_TPG_NAME),
                product.getTiePointGrid(SCENE_AZIMUTH_TPG_NAME),
                null);
    }
}
