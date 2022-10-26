package org.esa.snap.opt.enmap;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.datamodel.*;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

import static org.esa.snap.opt.enmap.EnmapFileUtils.*;

class EnmapProductReader extends AbstractProductReader {
    public static final int KM_IN_METERS = 1000;
    public static final String SCENE_AZIMUTH_TPG_NAME = "scene_azimuth";
    public static final String SUN_AZIMUTH_TPG_NAME = "sun_azimuth";
    public static final String SUN_ELEVATION_TPG_NAME = "sun_elevation";
    public static final String ACROSS_OFF_NADIR_TPG_NAME = "across_off_nadir";
    public static final String ALONG_OFF_NADIR_TPG_NAME = "along_off_nadir";

    private static final String CANNOT_READ_PRODUCT_MSG = "Cannot read product";
    private final Object syncObject;

    private VirtualDir dataDir;
    private final Map<String, RenderedImage> bandImageMap = new TreeMap<>();
    private final List<EnmapImageReader> imageReaderList = new ArrayList<>();

    public EnmapProductReader(EnmapProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        syncObject = new Object();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        Path path = InputTypes.toPath(super.getInput());
        if (!EnmapFileUtils.isZip(path)) {
            path = path.getParent();
        }
        dataDir = VirtualDir.create(path.toFile());
        if (dataDir == null) {
            throw new IOException(String.format("%s%nVirtual directory could not be created", CANNOT_READ_PRODUCT_MSG));
        }

        String[] fileNames = dataDir.listAllFiles();
        String metadataFile = getMetadataFile(fileNames);
        EnmapMetadata meta = EnmapMetadata.create(dataDir.getInputStream(metadataFile));

        Dimension dimension = meta.getSceneDimension();
        Product product = new Product(meta.getProductName(), meta.getProductType(), dimension.width, dimension.height);
        product.setStartTime(meta.getStartTime());
        product.setEndTime(meta.getStopTime());

        addGeoCoding(product, meta);
        addSpectralBands(product, meta);
        addTiePointGrids(product, meta);
        addQualityLayers(product, meta);
        addMetadata(product, meta);

        product.setAutoGrouping("band:PIXELMASK:QUALITY");

        return product;
    }

    private void addMetadata(Product product, EnmapMetadata meta) throws IOException {
        meta.insertInto(product.getMetadataRoot());
    }

    private void addQualityLayers(Product product, EnmapMetadata meta) throws IOException {
        // cannot be read correctly something is wrong with the GeoTIFF, see important note in the test data set.
        //addPixelMasksQl(product, dataDir, meta);
        addClassesQl(product, meta);
        addHazeQl(product, meta);
        addCloudQl(product, meta);
        addCirrusQl(product, meta);
        addCloudShadowQl(product, meta);
        addSnowQl(product, meta);
        addTestFlagsQl(product, meta);
    }

    private void addClassesQl(Product product, EnmapMetadata meta) throws IOException {
        String qualityKey = QUALITY_CLASSES_KEY;
        FlagCoding flagCoding = new FlagCoding(qualityKey);
        product.getFlagCodingGroup().add(flagCoding);
        QualityLayerInfo.QL_CLASSES_LAND.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CLASSES_LAND.addMaskTo(product);
        QualityLayerInfo.QL_CLASSES_WATER.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CLASSES_WATER.addMaskTo(product);
        QualityLayerInfo.QL_CLASSES_BG.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CLASSES_BG.addMaskTo(product);

        EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
        imageReaderList.add(qualityReader); // prevents finalising the reader

        addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
    }

    private void addCloudQl(Product product, EnmapMetadata meta) throws IOException {
        String qualityKey = QUALITY_CLOUD_KEY;
        FlagCoding flagCoding = new FlagCoding(qualityKey);
        product.getFlagCodingGroup().add(flagCoding);
        QualityLayerInfo.QL_CLOUD_CLOUD.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CLOUD_CLOUD.addMaskTo(product);

        EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
        imageReaderList.add(qualityReader); // prevents finalising the reader

        addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
    }

    private void addCloudShadowQl(Product product, EnmapMetadata meta) throws IOException {
        String qualityKey = QUALITY_CLOUDSHADOW_KEY;
        FlagCoding flagCoding = new FlagCoding(qualityKey);
        product.getFlagCodingGroup().add(flagCoding);
        QualityLayerInfo.QL_CLOUDSHADOW_SHADOW.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CLOUDSHADOW_SHADOW.addMaskTo(product);

        EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
        imageReaderList.add(qualityReader); // prevents finalising the reader

        addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
    }

    private void addHazeQl(Product product, EnmapMetadata meta) throws IOException {
        String qualityKey = QUALITY_HAZE_KEY;
        FlagCoding flagCoding = new FlagCoding(qualityKey);
        product.getFlagCodingGroup().add(flagCoding);
        QualityLayerInfo.QL_HAZE_HAZE.addFlagTo(flagCoding);
        QualityLayerInfo.QL_HAZE_HAZE.addMaskTo(product);

        EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
        imageReaderList.add(qualityReader); // prevents finalising the reader

        addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
    }

    private void addCirrusQl(Product product, EnmapMetadata meta) throws IOException {
        String qualityKey = QUALITY_CIRRUS_KEY;
        FlagCoding flagCoding = new FlagCoding(qualityKey);
        product.getFlagCodingGroup().add(flagCoding);
        QualityLayerInfo.QL_CIRRUS_THIN.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CIRRUS_THIN.addMaskTo(product);
        QualityLayerInfo.QL_CIRRUS_MEDIUM.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CIRRUS_MEDIUM.addMaskTo(product);
        QualityLayerInfo.QL_CIRRUS_THICK.addFlagTo(flagCoding);
        QualityLayerInfo.QL_CIRRUS_THICK.addMaskTo(product);

        EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
        imageReaderList.add(qualityReader); // prevents finalising the reader

        addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
    }

    private void addSnowQl(Product product, EnmapMetadata meta) throws IOException {
        String qualityKey = QUALITY_SNOW_KEY;
        FlagCoding flagCoding = new FlagCoding(qualityKey);
        product.getFlagCodingGroup().add(flagCoding);
        QualityLayerInfo.QL_SNOW_SNOW.addFlagTo(flagCoding);
        QualityLayerInfo.QL_SNOW_SNOW.addMaskTo(product);

        EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
        imageReaderList.add(qualityReader); // prevents finalising the reader

        addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
    }

    private void addPixelMasksQl(Product product, VirtualDir dataDir, EnmapMetadata meta) throws IOException {
        EnmapImageReader pixelMaskReader = EnmapImageReader.createPixelMaskReader(dataDir, meta);
        imageReaderList.add(pixelMaskReader);
        if (EnmapMetadata.PROCESSING_LEVEL.L1B.equals(meta.getProcessingLevel())) {
            int numVnirImages = meta.getNumVnirBands();
            String vnirQualityKey = QUALITY_PIXELMASK_VNIR_KEY;
            FlagCoding vnirFlagCoding = new FlagCoding(vnirQualityKey);
            product.getFlagCodingGroup().add(vnirFlagCoding);
            QualityLayerInfo.QL_PM_VNIR_DEFECTIVE_SERIES.addFlagSeriesTo(vnirFlagCoding, numVnirImages);
            QualityLayerInfo.QL_PM_VNIR_DEFECTIVE_SERIES.addMaskSeriesTo(product, numVnirImages);

            for (int i = 0; i < numVnirImages; i++) {
                RenderedImage imageAt = pixelMaskReader.getImageAt(i);
                addFlagBand(product, vnirFlagCoding, String.format("%s_%03d", vnirQualityKey, i + 1), imageAt);
            }

            int numSwirImages = meta.getNumSwirBands();
            String swirQualityKey = QUALITY_PIXELMASK_SWIR_KEY;
            FlagCoding swirFlagCoding = new FlagCoding(swirQualityKey);
            product.getFlagCodingGroup().add(swirFlagCoding);
            QualityLayerInfo.QL_PM_SWIR_DEFECTIVE_SERIES.addFlagSeriesTo(swirFlagCoding, numSwirImages);
            QualityLayerInfo.QL_PM_SWIR_DEFECTIVE_SERIES.addMaskSeriesTo(product, numSwirImages);

            for (int i = numVnirImages; i < numVnirImages + numSwirImages; i++) {
                RenderedImage imageAt = pixelMaskReader.getImageAt(i);
                addFlagBand(product, swirFlagCoding, String.format("%s_%03d", swirQualityKey, i + 1), imageAt);
            }

        } else {
            int numImages = meta.getNumSpectralBands();
            String qualityKey = QUALITY_PIXELMASK_KEY;
            FlagCoding flagCoding = new FlagCoding(qualityKey);
            product.getFlagCodingGroup().add(flagCoding);

            QualityLayerInfo.QL_PM_DEFECTIVE_SERIES.addFlagSeriesTo(flagCoding, numImages);
            QualityLayerInfo.QL_PM_DEFECTIVE_SERIES.addMaskSeriesTo(product, numImages);
            for (int i = 0; i < numImages; i++) {
                RenderedImage imageAt = pixelMaskReader.getImageAt(i);
                addFlagBand(product, flagCoding, String.format("%s_%03d", qualityKey, i + 1), imageAt);
            }

        }
    }

    private void addTestFlagsQl(Product product, EnmapMetadata meta) throws IOException {
        if (EnmapMetadata.PROCESSING_LEVEL.L1B.equals(meta.getProcessingLevel())) {
            String vnirQualityKey = QUALITY_TESTFLAGS_VNIR_KEY;
            FlagCoding vnirFlagCoding = new FlagCoding(vnirQualityKey);
            product.getFlagCodingGroup().add(vnirFlagCoding);

            QualityLayerInfo.QL_TF_VNIR_NOMINAL.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_NOMINAL.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_REDUCED.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_REDUCED.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_LOW.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_LOW.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_NOT.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_NOT.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_INTERPOLATED_SWIR.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_INTERPOLATED_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_INTERPOLATED_VNIR.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_INTERPOLATED_VNIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_SATURATION_SWIR.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_SATURATION_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_SATURATION_VNIR.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_SATURATION_VNIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_ARTEFACT_SWIR.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_ARTEFACT_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_VNIR_ARTEFACT_VNIR.addFlagTo(vnirFlagCoding);
            QualityLayerInfo.QL_TF_VNIR_ARTEFACT_VNIR.addMaskTo(product);

            EnmapImageReader qualityVnirReader = EnmapImageReader.createQualityReader(dataDir, meta, vnirQualityKey);
            imageReaderList.add(qualityVnirReader); // prevents finalising the reader

            addFlagBand(product, vnirFlagCoding, vnirQualityKey, qualityVnirReader.getImageAt(0));

            String swirQualityKey = QUALITY_TESTFLAGS_SWIR_KEY;
            FlagCoding swirFlagCoding = new FlagCoding(swirQualityKey);
            product.getFlagCodingGroup().add(swirFlagCoding);

            QualityLayerInfo.QL_TF_SWIR_NOMINAL.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_NOMINAL.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_REDUCED.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_REDUCED.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_LOW.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_LOW.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_NOT.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_NOT.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_INTERPOLATED_SWIR.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_INTERPOLATED_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_INTERPOLATED_VNIR.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_INTERPOLATED_VNIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_SATURATION_SWIR.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_SATURATION_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_SATURATION_VNIR.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_SATURATION_VNIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_ARTEFACT_SWIR.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_ARTEFACT_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SWIR_ARTEFACT_VNIR.addFlagTo(swirFlagCoding);
            QualityLayerInfo.QL_TF_SWIR_ARTEFACT_VNIR.addMaskTo(product);

            EnmapImageReader qualitySwirReader = EnmapImageReader.createQualityReader(dataDir, meta, swirQualityKey);
            imageReaderList.add(qualitySwirReader); // prevents finalising the reader

            addFlagBand(product, swirFlagCoding, swirQualityKey, qualitySwirReader.getImageAt(0));
        } else {
            String qualityKey = QUALITY_TESTFLAGS_KEY;
            FlagCoding flagCoding = new FlagCoding(qualityKey);
            product.getFlagCodingGroup().add(flagCoding);
            QualityLayerInfo.QL_TF_NOMINAL.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_NOMINAL.addMaskTo(product);
            QualityLayerInfo.QL_TF_REDUCED.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_REDUCED.addMaskTo(product);
            QualityLayerInfo.QL_TF_LOW.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_LOW.addMaskTo(product);
            QualityLayerInfo.QL_TF_NOT.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_NOT.addMaskTo(product);
            QualityLayerInfo.QL_TF_INTERPOLATED_SWIR.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_INTERPOLATED_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_INTERPOLATED_VNIR.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_INTERPOLATED_VNIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SATURATION_SWIR.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_SATURATION_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_SATURATION_VNIR.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_SATURATION_VNIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_ARTEFACT_SWIR.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_ARTEFACT_SWIR.addMaskTo(product);
            QualityLayerInfo.QL_TF_ARTEFACT_VNIR.addFlagTo(flagCoding);
            QualityLayerInfo.QL_TF_ARTEFACT_VNIR.addMaskTo(product);

            EnmapImageReader qualityReader = EnmapImageReader.createQualityReader(dataDir, meta, qualityKey);
            imageReaderList.add(qualityReader); // prevents finalising the reader

            addFlagBand(product, flagCoding, qualityKey, qualityReader.getImageAt(0));
        }
    }

    private void addFlagBand(Product product, FlagCoding flagCoding, String qualityKey, RenderedImage tiffImage) {
        Band flagBand = new Band(qualityKey, ProductData.TYPE_UINT8, tiffImage.getWidth(), tiffImage.getHeight());
        flagBand.setSampleCoding(flagCoding);
        // first the band needs to be added to the product and only then the source mage set
        // see: https://senbox.atlassian.net/browse/SNAP-935
        product.addBand(flagBand);
        flagBand.setSourceImage(tiffImage);
    }

    private void addTiePointGrids(Product product, EnmapMetadata meta) throws IOException {
        addTPG(product, SCENE_AZIMUTH_TPG_NAME, meta.getSceneAzimuthAngles());
        addTPG(product, SUN_AZIMUTH_TPG_NAME, meta.getSunAzimuthAngles());
        addTPG(product, SUN_ELEVATION_TPG_NAME, meta.getSunElevationAngles());
        addTPG(product, ACROSS_OFF_NADIR_TPG_NAME, meta.getAcrossOffNadirAngles());
        addTPG(product, ALONG_OFF_NADIR_TPG_NAME, meta.getAlongOffNadirAngles());
    }

    private static TiePointGrid addTPG(Product product, String tpgName, double[] tpgValue) {
        int gridWidth = 2;
        int gridHeight = 2;
        int gridSamplingX = product.getSceneRasterWidth();
        int gridSamplingY = product.getSceneRasterHeight();
        float[] tpData = new float[tpgValue.length];
        IntStream.range(0, tpgValue.length).forEach(i -> tpData[i] = (float) tpgValue[i]);
        TiePointGrid tpg = new TiePointGrid(tpgName, gridWidth, gridHeight, 0, 0,
                gridSamplingX, gridSamplingY, tpData, true);
        tpg.setUnit("DEG");
        product.addTiePointGrid(tpg);
        return tpg;
    }

    /* NOTE!
    Using the images provided by the GeoTiffImageReader leads to threading artifacts in the image. When using
    the GeoTiffProductReader the data handling is very slow, because of bad tiling. 512x512 tile-size is too big for
    more than 200 bands. The solution is to use the image in the readBandRasterData method to read the data by
    synchronize the access to the GeoTiffImageReader.
     */
    private void addSpectralBands(Product product, EnmapMetadata meta) throws IOException {

        EnmapImageReader spectralImageReader = EnmapImageReader.createSpectralReader(dataDir, meta);
        imageReaderList.add(spectralImageReader);

        product.setPreferredTileSize(spectralImageReader.getTileDimension());
        int[] spectralIndices = joinArrays(meta.getVnirIndices(), meta.getSwirIndices());
        int dataType = ProductData.TYPE_INT16;
        for (int i = 0; i < spectralImageReader.getNumImages(); i++) {
            String bandName = String.format("band_%03d", spectralIndices[i]);
            Band band = new Band(bandName, dataType, product.getSceneRasterWidth(), product.getSceneRasterHeight());
            band.setSpectralBandIndex(spectralIndices[i] - 1);
            band.setSpectralWavelength(meta.getCentralWavelength(i));
            band.setSpectralBandwidth(meta.getBandwidth(i));
            band.setDescription(meta.getSpectralBandDescription(i));
            band.setUnit(meta.getSpectralUnit());
            band.setScalingFactor(meta.getBandScaling(i));
            band.setScalingOffset(meta.getBandOffset(i));
            band.setNoDataValue(meta.getSpectralBackgroundValue());
            band.setNoDataValueUsed(true);
            bandImageMap.put(bandName, spectralImageReader.getImageAt(i));
            product.addBand(band);
        }

    }

    private int[] joinArrays(int[] vnirIndices, int[] swirIndices) {
        return IntStream.concat(Arrays.stream(vnirIndices), Arrays.stream(swirIndices)).toArray();
    }

    private void addGeoCoding(Product product, EnmapMetadata meta) throws IOException {
        switch (meta.getProcessingLevel()) {
            case L1B:
                addTiePointGeoCoding(product, meta);
                break;
            case L1C:
            case L2A:
                addCrsGeoCoding(product, meta);
                break;
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) {
        int[] samples;
        synchronized (syncObject) {
            RenderedImage renderedImage = bandImageMap.get(destBand.getName());
            Raster data = renderedImage.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
            samples = data.getSamples(destOffsetX, destOffsetY, destWidth, destHeight, 0, (int[]) null);
        }
        IntStream.range(0, samples.length).parallel().forEach(i -> destBuffer.setElemIntAt(i, samples[i]));

    }

    @Override
    public void close() {
        for (EnmapImageReader geoTiffImageReader : imageReaderList) {
            geoTiffImageReader.close();
        }
        imageReaderList.clear();

        if (dataDir != null) {
            dataDir.close();
        }
    }

    private void addCrsGeoCoding(Product product, EnmapMetadata meta) throws IOException {
        GeoReferencing geoReferencing = meta.getGeoReferencing();
        try {
            String epsgCode = getEPSGCode(geoReferencing.projection);
            CoordinateReferenceSystem coordinateReferenceSystem = CRS.decode(epsgCode);
            Dimension dimension = meta.getSceneDimension();
            double resolution = geoReferencing.resolution;
            // todo - easting and northing should be provided in metadata but are not in the test data
            // todo - we need to read it from one of the geotiff files.
//                double easting = geoReferencing.easting;
//                double northing = geoReferencing.northing;
            Point2D eastingNorthing = getEastingNorthing(meta);
            if (eastingNorthing != null) {
                CrsGeoCoding crsGeoCoding = new CrsGeoCoding(coordinateReferenceSystem,
                        (int) dimension.getWidth(), (int) dimension.getHeight(),
                        eastingNorthing.getX(), eastingNorthing.getY(),
                        resolution, resolution, geoReferencing.refX, geoReferencing.refY);
                product.setSceneGeoCoding(crsGeoCoding);
            }
        } catch (Exception e) {
            throw new IOException(CANNOT_READ_PRODUCT_MSG, e);
        }
    }

    private static void addTiePointGeoCoding(Product product, EnmapMetadata meta) throws IOException {
        String lonName = "longitude";
        String latName = "latitude";
        double[] cornerLatitudes = meta.getCornerLatitudes();
        double[] cornerLongitudes = meta.getCornerLongitudes();
        addTPG(product, latName, cornerLatitudes);
        TiePointGrid lonGrid = addTPG(product, lonName, cornerLongitudes);
        double pixelSizeKm = meta.getPixelSize() / KM_IN_METERS;
        GeoRaster geoRaster = new GeoRaster(cornerLongitudes, cornerLatitudes,
                lonName, latName, lonGrid.getGridWidth(), lonGrid.getGridHeight(),
                product.getSceneRasterWidth(), product.getSceneRasterHeight(), pixelSizeKm,
                lonGrid.getOffsetX(), lonGrid.getOffsetY(),
                lonGrid.getSubSamplingX(), lonGrid.getSubSamplingY());
        ComponentGeoCoding sceneGeoCoding = new ComponentGeoCoding(geoRaster,
                ComponentFactory.getForward(TiePointBilinearForward.KEY),
                ComponentFactory.getInverse(TiePointInverse.KEY), GeoChecks.ANTIMERIDIAN, DefaultGeographicCRS.WGS84);
        sceneGeoCoding.initialize();
        product.setSceneGeoCoding(sceneGeoCoding);
    }

    private Point2D getEastingNorthing(EnmapMetadata meta) throws IOException {
        Map<String, String> fileNameMap = meta.getFileNameMap();
        String dataFileName = fileNameMap.get(QUALITY_CLASSES_KEY);
        InputStream inputStream = EnmapFileUtils.getInputStream(dataDir,dataFileName);
        ProductReader reader = null;
        try {
            reader = ProductIO.getProductReader("GeoTIFF");
            Product product = reader.readProductNodes(inputStream, null);
            GeoCoding geoCoding = product.getSceneGeoCoding();
            MathTransform i2m = geoCoding.getImageToMapTransform();
            if (i2m instanceof AffineTransform) {
                AffineTransform i2mAT = (AffineTransform) i2m;
                return new Point2D.Double(i2mAT.getTranslateX(), i2mAT.getTranslateY());
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    private static String getEPSGCode(String projection) throws Exception {
        int code;
        if (projection.startsWith("UTM")) {
            int utmZone = Integer.parseInt(projection.substring(8, 10));
            if (projection.endsWith("North")) {
                code = 32600 + utmZone;
            } else {
                code = 32700 + utmZone;
            }
        } else if (projection.equals("LAEA-ETRS89")) {
            code = 3035;
        } else if (projection.equals("Geographic")) {
            code = 4326;
        } else {
            throw new Exception(String.format("Cannot decode EPSG code from projection string '%s'", projection));
        }
        return "EPSG:" + code;
    }

    private String getMetadataFile(String[] fileNames) throws IOException {
        Optional<String> first = Arrays.stream(fileNames).filter(s -> s.endsWith(EnmapFileUtils.METADATA_SUFFIX)).findFirst();
        return first.orElseThrow(() -> new IOException("Metadata file not found"));
    }


}