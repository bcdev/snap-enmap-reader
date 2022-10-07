package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.ProductData;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.awt.*;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

class EnmapMetadataImpl implements EnmapMetadata {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    private final XPath xpath;
    private final Document doc;

    EnmapMetadataImpl(Document doc) {
        this.doc = doc;
        xpath = XPathFactory.newInstance().newXPath();
    }

    @Override
    public String getSchemaVersion() throws IOException {
        return getNodeContent("/level_X/metadata/schema/versionSchema");
    }

    @Override
    public String getProcessingVersion() throws IOException {
        return getNodeContent("/level_X/base/revision");
    }

    @Override
    public String getL0ProcessingVersion() throws IOException {
        return getNodeContent("/level_X/base/archivedVersion");
    }

    @Override
    public String getProductName() throws IOException {
        String fileName = getNodeContent("/level_X/metadata/name");
        return fileName.substring(0, 73);
    }

    @Override
    public ProductData.UTC getStartTime() throws IOException {
        return parseTimeString(getNodeContent("/level_X/base/temporalCoverage/startTime"));
    }

    @Override
    public String getProductType() throws IOException {
        return getNodeContent("/level_X/base/format");
    }

    @Override
    public String getProductFormat() throws IOException {
        return getNodeContent("/level_X/processing/productFormat");
    }

    @Override
    public Dimension getSceneDimension() throws IOException {
        int width = Integer.parseInt(getNodeContent("/level_X/specific/widthOfScene"));
        int height = Integer.parseInt(getNodeContent("/level_X/specific/heightOfScene"));
        return new Dimension(width, height);
    }

    @Override
    public Geometry getSpatialCoverage() throws IOException {
        double[] lats = getDoubleValues("/level_X/base/spatialCoverage/boundingPolygon/*/latitude", 5);
        double[] lons = getDoubleValues("/level_X/base/spatialCoverage/boundingPolygon/*/longitude", 5);
        // in L2A metadata the 5 coordinate is not equal to the first. The precision is higher.
        lats[4] = lats[0];
        lons[4] = lons[0];

        return createPolygon(lats, lons);
    }

    @Override
    public Geometry getSpatialOrthoCoverage() throws IOException {
        double[] lats = getDoubleValues("/level_X/specific/spatialCoverageOfOrthoScene/boundingPolygon/*/latitude", 5);
        double[] lons = getDoubleValues("/level_X/specific/spatialCoverageOfOrthoScene/boundingPolygon/*/longitude", 5);
        return createPolygon(lats, lons);
    }

    @Override
    public GeoReferencing getGeoReferencing() throws IOException {
        String projection = getNodeContent("/level_X/product/ortho/projection");
        String resString = getNodeContent("/level_X/product/ortho/resolution");
        double resolution = Double.NaN;
        if (!NOT_AVAILABLE.equalsIgnoreCase(resString)) {
            resolution = Double.parseDouble(resString);
        }
        return new GeoReferencing(projection, resolution);
    }

    @Override
    public double[] getSunElevationAngles() throws IOException {
        return getDoubleValues("/level_X/specific/sunElevationAngle/*", 4);
    }

    @Override
    public double getSunElevationAngleCenter() throws IOException {
        return getAngleCenter("sunElevationAngle");
    }

    @Override
    public double[] getSunAzimuthAngles() throws IOException {
        return getDoubleValues("/level_X/specific/sunAzimuthAngle/*", 4);
    }

    @Override
    public double getSunAzimuthAngleCenter() throws IOException {
        return getAngleCenter("sunAzimuthAngle");
    }

    @Override
    public double[] getAcrossOffNadirAngles() throws IOException {
        return getDoubleValues("/level_X/specific/acrossOffNadirAngle/*", 4);
    }

    @Override
    public double getAcrossOffNadirAngleCenter() throws IOException {
        return getAngleCenter("acrossOffNadirAngle");
    }

    @Override
    public double[] getAlongOffNadirAngles() throws IOException {
        return getDoubleValues("/level_X/specific/alongOffNadirAngle/*", 4);
    }

    @Override
    public double getAlongOffNadirAngleCenter() throws IOException {
        return getAngleCenter("alongOffNadirAngle");
    }

    @Override
    public double[] getSceneAzimuthAngles() throws IOException {
        return getDoubleValues("/level_X/specific/sceneAzimuthAngle/*", 4);
    }

    @Override
    public double getSceneAzimuthAngleCenter() throws IOException {
        return getAngleCenter("sceneAzimuthAngle");
    }

    private double[] getDoubleValues(String path, int count) throws IOException {
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

    @Override
    public ProductData.UTC getStopTime() throws IOException {
        String time = getNodeContent("/level_X/base/temporalCoverage/stopTime");
        return parseTimeString(time);
    }

    // todo - this method is borrowed from ISO8601Converter class in SNAP 10 (snap-core)
    // todo - when using SNAP 10 this method should be replaced
    private static ProductData.UTC parseTimeString(String iso8601String) {
        final TemporalAccessor accessor = FORMATTER.parse(iso8601String);
        final ZonedDateTime time = ZonedDateTime.from(accessor);
        final Date date = Date.from(time.toInstant());
        return ProductData.UTC.create(date, time.get(ChronoField.MICRO_OF_SECOND));
    }

    private String getNodeContent(String path) throws IOException {
        Node node = getNode(path);
        return node.getTextContent();
    }

    private Node getNode(String path) throws IOException {
        try {
            XPathExpression expr = xpath.compile(path);
            return (Node) expr.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new IOException(String.format("Not able to read metadata from xml path '%s'", path), e);
        }
    }

    private NodeList getNodeSet(String path) throws IOException {
        try {
            XPathExpression expr = xpath.compile(path);
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IOException(String.format("Not able to read metadata from xml path '%s'", path), e);
        }
    }

    private Geometry createPolygon(double[] lats, double[] lons) {
        Coordinate[] coords = new Coordinate[lats.length];
        for (int i = 0; i < lats.length; i++) {
            double lat = lats[i];
            double lon = lons[i];
            coords[i] = new Coordinate(lon, lat);
        }
        return new GeometryFactory().createPolygon(coords);
    }
}
