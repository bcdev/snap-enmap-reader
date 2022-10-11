package org.esa.snap.opt.dataio.enmap;

import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class EnmapL2AMetadataImpl extends EnmapMetadata {

    EnmapL2AMetadataImpl(Document doc, XPath xPath) {
        super(doc, xPath);
    }

    @Override
    public Dimension getSceneDimension() throws IOException {
        int width = Integer.parseInt(getNodeContent("/level_X/specific/widthOfOrthoScene"));
        int height = Integer.parseInt(getNodeContent("/level_X/specific/heightOfOrthoScene"));
        return new Dimension(width, height);
    }

    public Geometry getSpatialCoverage() throws IOException {
        double[] lats = getDoubleValues("/level_X/base/spatialCoverage/boundingPolygon/*/latitude", 5);
        double[] lons = getDoubleValues("/level_X/base/spatialCoverage/boundingPolygon/*/longitude", 5);
        // in L2A metadata the 5th coordinate is not equal to the first. The precision is higher.
        lats[4] = lats[0];
        lons[4] = lons[0];
        return createPolygon(lats, lons);
    }

    public float getSpectralBackgroundValue(int index) throws IOException {
        // For L2A the background value is not available or wrong in the testdata
        return -32768;
    }

    @Override
    public Map<String, String> getFileNameMap() throws IOException {
        NodeList nodeSet = getNodeSet("/level_X/product/productFileInformation/*/name");

        HashMap<String, String> map = new HashMap<>();
        map.put(EnmapFileUtils.METADATA_KEY, getFileName(EnmapFileUtils.METADATA_KEY, nodeSet));
        map.put(EnmapFileUtils.SPECTRAL_IMAGE_KEY, getFileName(EnmapFileUtils.SPECTRAL_IMAGE_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_CLASSES_KEY, getFileName(EnmapFileUtils.QUALITY_CLASSES_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_CLOUD_KEY, getFileName(EnmapFileUtils.QUALITY_CLOUD_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_CLOUDSHADOW_KEY, getFileName(EnmapFileUtils.QUALITY_CLOUDSHADOW_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_HAZE_KEY, getFileName(EnmapFileUtils.QUALITY_HAZE_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_CIRRUS_KEY, getFileName(EnmapFileUtils.QUALITY_CIRRUS_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_SNOW_KEY, getFileName(EnmapFileUtils.QUALITY_SNOW_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_TESTFLAGS_KEY, getFileName(EnmapFileUtils.QUALITY_TESTFLAGS_KEY, nodeSet));
        map.put(EnmapFileUtils.QUALITY_PIXELMASK_KEY, getFileName(EnmapFileUtils.QUALITY_PIXELMASK_KEY, nodeSet));
        return map;
    }

}
