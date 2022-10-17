package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;
import org.geotools.referencing.CRS;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static org.esa.snap.opt.dataio.enmap.EnmapFileUtils.QUALITY_CLASSES_KEY;
import static org.esa.snap.opt.dataio.enmap.EnmapFileUtils.SPECTRAL_IMAGE_KEY;

class EnmapProductReader extends AbstractProductReader {
    public static final String CANNOT_READ_PRODUCT_MSG = "Cannot read product";
    private VirtualDir dataDir;
    private SpectralImageReader spectralImageReader;

    private final Map<String, RenderedImage> bandImageMap = new TreeMap<>();

    public EnmapProductReader(EnmapProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
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

        return product;
    }

    private void addTiePointGrids(Product product, EnmapMetadata meta) throws IOException {
        addTPG(product, "scene_azimuth", meta.getSceneAzimuthAngles());
        addTPG(product, "sun_azimuth", meta.getSunAzimuthAngles());
        addTPG(product, "sun_elevation", meta.getSunElevationAngles());
        addTPG(product, "across_off_nadir", meta.getAcrossOffNadirAngles());
        addTPG(product, "along_off_nadir", meta.getAlongOffNadirAngles());
    }

    private static void addTPG(Product product, String tpgName, double[] sceneAzimuthAngles) {
        int gridWidth = 2;
        int gridHeight = 2;
        int gridSamplingX = product.getSceneRasterWidth();
        int gridSamplingY = product.getSceneRasterHeight();
        float[] tpData = new float[sceneAzimuthAngles.length];
        IntStream.range(0, sceneAzimuthAngles.length).forEach(i -> tpData[i] = (float) sceneAzimuthAngles[i]);
        TiePointGrid tpg = new TiePointGrid(tpgName, gridWidth, gridHeight, 0, 0,
                gridSamplingX, gridSamplingY, tpData, true);
        tpg.setUnit("DEG");
        product.addTiePointGrid(tpg);
    }

    /* NOTE!
    Using the images provided by the GeoTiffImageReader leads to threading artifacts in the image. When using
    the GeoTiffProductReader the data handling is very slow, because of bad tiling. 512x512 tile-size is too big for
    more than 200 bands. The solution is to use the image in the readBandRasterData method to read the data by
    synchronize the access to the GeoTiffImageReader.
     */
    private void addSpectralBands(Product product, EnmapMetadata meta) throws IOException {

        spectralImageReader = SpectralImageReader.create(dataDir, meta);
        product.setPreferredTileSize(spectralImageReader.getTileDimension());

        int dataType = ProductData.TYPE_INT16;
        int numBands = meta.getNumSpectralBands();
        for (int i = 0; i < numBands; i++) {
            String bandName = String.format("band_%d", i + 1);
            Band band = new Band(bandName, dataType, product.getSceneRasterWidth(), product.getSceneRasterHeight());
            band.setSpectralBandIndex(i);
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

    private void addGeoCoding(Product product, EnmapMetadata meta) throws IOException {
        String productFormat = meta.getProductType();
        if (productFormat.endsWith("L2A")) {
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
    }

    private Point2D getEastingNorthing(EnmapMetadata meta) throws IOException {
        Map<String, String> fileNameMap = meta.getFileNameMap();
        String dataFileName = fileNameMap.get(QUALITY_CLASSES_KEY);
        InputStream inputStream = dataDir.getInputStream(dataFileName);
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
        int code = -1;
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

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {
        int[] samples;
        synchronized (spectralImageReader) {
            RenderedImage renderedImage = bandImageMap.get(destBand.getName());
            Raster data = renderedImage.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
            samples = data.getSamples(destOffsetX, destOffsetY, destWidth, destHeight, 0, (int[]) null);
        }
        IntStream.range(0, samples.length).parallel().forEach(i -> destBuffer.setElemIntAt(i, samples[i]));

    }

    @Override
    public void close() throws IOException {
        if (spectralImageReader != null) {
            spectralImageReader.close();
        }
        if (dataDir != null) {
            dataDir.close();
        }
    }

}
