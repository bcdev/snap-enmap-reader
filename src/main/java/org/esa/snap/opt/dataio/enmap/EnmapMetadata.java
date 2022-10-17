package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Map;

public abstract class EnmapMetadata {
    static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX";
    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    private final XPath xpath;
    private final Document doc;

    enum PROCESSING_LEVEL {L1B, L1C, L2A}

    String BSQ_Metadata = "BSQ+Metadata";
    String BIL_Metadata = "BIL+Metadata";
    String BIP_Metadata = "BIP+Metadata";
    String J2K_Metadata = "JPEG2000+Metadata";
    String GTF_Metadata = "GeoTIFF+Metadata";
    String NOT_AVAILABLE = "NA";

    protected EnmapMetadata(Document doc, XPath xpath) {
        this.xpath = xpath;
        this.doc = doc;
    }

    static EnmapMetadata create(Path metadataPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(metadataPath)) {
            return create(inputStream);
        }
    }

    static EnmapMetadata create(InputStream inputStream) throws IOException {
        Document xmlDocument = EnmapMetadata.createXmlDocument(inputStream);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String processingLevel = getProcessingLevel(xPath, xmlDocument);
        switch (PROCESSING_LEVEL.valueOf(processingLevel)) {
            case L1B:
                return new EnmapL1BMetadataImpl(xmlDocument, xPath);
            case L1C:
                return new EnmapL1CMetadataImpl(xmlDocument, xPath);
            case L2A:
                return new EnmapL2AMetadataImpl(xmlDocument, xPath);
            default:
                throw new IOException(String.format("Unknown product level '%s'", processingLevel));
        }
    }

    static Document createXmlDocument(InputStream inputStream) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(inputStream);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Cannot create document from manifest XML file.", e);
        }
    }

    // todo - this method is borrowed from ISO8601Converter class in SNAP 10 (snap-core)
    // todo - when using SNAP 10 this method should be replaced
    static ProductData.UTC parseTimeString(String iso8601String) {
        final TemporalAccessor accessor = EnmapMetadata.FORMATTER.parse(iso8601String);
        final ZonedDateTime time = ZonedDateTime.from(accessor);
        final Date date = Date.from(time.toInstant());
        return ProductData.UTC.create(date, time.get(ChronoField.MICRO_OF_SECOND));
    }

    /**
     * The version of the XML schema.
     *
     * @return the version
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getSchemaVersion() throws IOException {
        return getNodeContent("/level_X/metadata/schema/versionSchema");
    }

    /**
     * The version of the processing chain.
     *
     * @return the version
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getProcessingVersion() throws IOException {
        return getNodeContent("/level_X/base/revision");
    }

    /**
     * The version of the processor used to generate the archived L0 product.
     *
     * @return the version
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getL0ProcessingVersion() throws IOException {
        return getNodeContent("/level_X/base/archivedVersion");
    }

    /**
     * The processing level of the product, one of {@code [L1B, L1C, L2A]}
     *
     * @return the processing level
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getProcessingLevel() throws IOException {
        return getProcessingLevel(xpath, doc);
    }

    private static String getProcessingLevel(XPath xPath, Document xmlDocument) throws IOException {
        return getNodeContent("/level_X/base/level", xPath, xmlDocument);
    }

    /**
     * The name of the product
     *
     * @return the name
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getProductName() throws IOException {
        String fileName = getNodeContent("/level_X/metadata/name");
        return fileName.substring(0, 73);
    }

    /**
     * The type string of the product
     *
     * @return the product type
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getProductType() throws IOException {
        return getNodeContent("/level_X/base/format");
    }

    /**
     * The data format of the product contents.
     * Is one of the following values {@code [BSQ+Metadata, BIL+Metadata, BIP+Metadata, JPEG2000+Metadata, GeoTIFF+Metadata]}
     *
     * @return the data format
     * @throws IOException in case the metadata XML file could not be read
     */
    public String getProductFormat() throws IOException {
        return getNodeContent("/level_X/processing/productFormat");
    }

    /**
     * The sensing start time of the scene at UTC time zone
     *
     * @return the sensing start time
     * @throws IOException in case the metadata XML file could not be read
     */
    public ProductData.UTC getStartTime() throws IOException {
        return EnmapMetadata.parseTimeString(getNodeContent("/level_X/base/temporalCoverage/startTime"));
    }

    /**
     * The sensing stop time of the scene at UTC time zone
     *
     * @return the sensing stop time
     * @throws IOException in case the metadata XML file could not be read
     */
    public ProductData.UTC getStopTime() throws IOException {
        return EnmapMetadata.parseTimeString(getNodeContent("/level_X/base/temporalCoverage/stopTime"));
    }

    /**
     * The widht and height of the scene as dimension object
     *
     * @return the dimension of the scene
     * @throws IOException in case the metadata XML file could not be read
     */
    abstract Dimension getSceneDimension() throws IOException;

    /**
     * Returns the spatial coverage of the scene raster in WGS84 coordinates for the satellite raster (Level L1B)
     *
     * @return the spatial coverage
     * @throws IOException in case the metadata XML file could not be read
     */
    public Geometry getSpatialCoverage() throws IOException {
        double[] lats = getDoubleValues("/level_X/base/spatialCoverage/boundingPolygon/*/latitude", 5);
        double[] lons = getDoubleValues("/level_X/base/spatialCoverage/boundingPolygon/*/longitude", 5);
        // in L2A metadata the 5th coordinate is not equal to the first. The precision is higher.
        lats[4] = lats[0];
        lons[4] = lons[0];

        return createPolygon(lats, lons);
    }

    /**
     * Returns the spatial coverage of the reprojected and orthorectified scene (Level L1C and L2A) in WGS84 coordinates,
     * regardless of the coordinate reference system.
     *
     * @return the spatial coverage
     * @throws IOException in case the metadata XML file could not be read
     */
    public Geometry getSpatialOrthoCoverage() throws IOException {
        double[] lats = getDoubleValues("/level_X/specific/spatialCoverageOfOrthoScene/boundingPolygon/*/latitude", 5);
        double[] lons = getDoubleValues("/level_X/specific/spatialCoverageOfOrthoScene/boundingPolygon/*/longitude", 5);
        return createPolygon(lats, lons);
    }

    /**
     * Returns the geo-referencing information of the reprojected and orthorectified scene (Level L1C and L2A)
     *
     * @throws IOException in case the metadata XML file could not be read
     */
    public GeoReferencing getGeoReferencing() throws IOException {
        String projection = getNodeContent("/level_X/product/ortho/projection");
        String resString = getNodeContent("/level_X/product/ortho/resolution");
        double resolution = Double.NaN;
        if (!NOT_AVAILABLE.equalsIgnoreCase(resString)) {
            resolution = Double.parseDouble(resString);
        }
        return new GeoReferencing(projection, resolution);
    }


    /**
     * returns the sun elevation angles at the four corner points of the scene
     *
     * @return an array containing the sun elevation angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    public double[] getSunElevationAngles() throws IOException {
        return getDoubleValues("/level_X/specific/sunElevationAngle/*", 4);
    }

    /**
     * The sun elevation angle at the center of the scene
     *
     * @return the sun elevation angle
     * @throws IOException in case the metadata XML file could not be read
     */
    public double getSunElevationAngleCenter() throws IOException {
        return getAngleCenter("sunElevationAngle");
    }

    /**
     * the sun azimuth angles at the four corner points of the scene
     *
     * @return an array containing the sun azimuth angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    public double[] getSunAzimuthAngles() throws IOException {
        return getDoubleValues("/level_X/specific/sunAzimuthAngle/*", 4);
    }

    /**
     * The sun azimuth angle at the center of the scene
     *
     * @return the sun azimuth angle
     * @throws IOException in case the metadata XML file could not be read
     */
    public double getSunAzimuthAngleCenter() throws IOException {
        return getAngleCenter("sunAzimuthAngle");
    }

    /**
     * The across off-nadir angles at the four corner points of the scene
     *
     * @return an array containing the across off-nadir angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    public double[] getAcrossOffNadirAngles() throws IOException {
        return getDoubleValues("/level_X/specific/acrossOffNadirAngle/*", 4);
    }

    /**
     * The across off-nadir angle at the center of the scene
     *
     * @return the across off-nadir angle
     * @throws IOException in case the metadata XML file could not be read
     */
    public double getAcrossOffNadirAngleCenter() throws IOException {
        return getAngleCenter("acrossOffNadirAngle");
    }

    /**
     * The along off-nadir angles at the four corner points of the scene
     *
     * @return an array containing the along off-nadir angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    public double[] getAlongOffNadirAngles() throws IOException {
        return getDoubleValues("/level_X/specific/alongOffNadirAngle/*", 4);
    }

    /**
     * The along off-nadir angle at the center of the scene
     *
     * @return the along off-nadir angle
     * @throws IOException in case the metadata XML file could not be read
     */
    public double getAlongOffNadirAngleCenter() throws IOException {
        return getAngleCenter("alongOffNadirAngle");
    }

    /**
     * The scene azimuth angles at the four corner points of the scene
     *
     * @return an array containing the scene azimuth angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    public double[] getSceneAzimuthAngles() throws IOException {
        return getDoubleValues("/level_X/specific/sceneAzimuthAngle/*", 4);
    }

    /**
     * The scene azimuth angle at the center of the scene
     *
     * @return the scene azimuth angle
     * @throws IOException in case the metadata XML file could not be read
     */
    public double getSceneAzimuthAngleCenter() throws IOException {
        return getAngleCenter("sceneAzimuthAngle");
    }

    /**
     * Returns a map of files of the product.
     *
     * @throws IOException in case the metadata XML file could not be read
     */
    abstract Map<String, String> getFileNameMap() throws IOException;

    public abstract int getNumSpectralBands() throws IOException;

    public float getCentralWavelength(int index) throws IOException {
        return Float.parseFloat(getNodeContent(String.format("/level_X/specific/bandCharacterisation/bandID[@number='%d']/wavelengthCenterOfBand", index + 1)));
    }

    public float getBandwidth(int index) throws IOException {
        return Float.parseFloat(getNodeContent(String.format("/level_X/specific/bandCharacterisation/bandID[@number='%d']/FWHMOfBand", index + 1)));
    }

    public float getBandScaling(int index) throws IOException {
        return Float.parseFloat(getNodeContent(String.format("/level_X/specific/bandCharacterisation/bandID[@number='%d']/GainOfBand", index + 1)));
    }

    public float getBandOffset(int index) throws IOException {
        return Float.parseFloat(getNodeContent(String.format("/level_X/specific/bandCharacterisation/bandID[@number='%d']/OffsetOfBand", index + 1)));
    }

    public float getSpectralBackgroundValue() throws IOException {
        return Float.parseFloat(getNodeContent("/level_X/specific/backgroundValue"));
    }

    /**
     * returns a description for the spectral channel at the specified index
     *
     * @return description for the specified spectral channel
     */
    public abstract String getSpectralBandDescription(int index) throws IOException;


    /**
     * Returns the physical unit of the spectral channels
     *
     * @return the physical Unit.
     */
    public abstract String getSpectralUnit();


    Node getNode(String path) throws IOException {
        return getNode(path, xpath, doc);
    }

    String getNodeContent(String path) throws IOException {
        return getNodeContent(path, xpath, doc);

    }

    private static String getNodeContent(String path, XPath xpath, Document doc) throws IOException {
        Node node = getNode(path, xpath, doc);
        if (node != null) {
            return node.getTextContent();
        } else {
            throw new IOException(String.format("Not able to read metadata from xml path '%s'", path));
        }
    }

    private static Node getNode(String path, XPath xpath, Document doc) throws IOException {
        try {
            XPathExpression expr = xpath.compile(path);
            return (Node) expr.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new IOException(String.format("Not able to read metadata from xml path '%s'", path), e);
        }
    }

    NodeList getNodeSet(String path) throws IOException {
        try {
            XPathExpression expr = xpath.compile(path);
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IOException(String.format("Not able to read metadata from xml path '%s'", path), e);
        }
    }

    Geometry createPolygon(double[] lats, double[] lons) {
        Coordinate[] coords = new Coordinate[lats.length];
        for (int i = 0; i < lats.length; i++) {
            double lat = lats[i];
            double lon = lons[i];
            coords[i] = new Coordinate(lon, lat);
        }
        return new GeometryFactory().createPolygon(coords);
    }

    double[] getDoubleValues(String path, int count) throws IOException {
        NodeList nodeSet = getNodeSet(path);
        double[] angles = new double[count];
        for (int i = 0; i < angles.length; i++) {
            angles[i] = Double.parseDouble(nodeSet.item(i).getTextContent());
        }
        return angles;
    }

    private double getAngleCenter(String alongOffNadirAngle) throws IOException {
        return Double.parseDouble(getNodeContent("/level_X/specific/" + alongOffNadirAngle + "/center"));
    }

    protected String getFileName(String key, NodeList fileNodeSet) {
        for (int i = 0; i < fileNodeSet.getLength(); i++) {
            String fileName = fileNodeSet.item(i).getTextContent();
            if (FileUtils.getFilenameWithoutExtension(fileName).endsWith(key)) {
                return fileName;
            }
        }
        return null;
    }
}
