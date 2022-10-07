package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.ProductData;
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
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class EnmapMetadata {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    private final XPath xpath;
    private final Document doc;

    private EnmapMetadata(Document doc) {
        this.doc = doc;
        xpath = XPathFactory.newInstance().newXPath();
    }

    public static EnmapMetadata create(Path metadataPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(metadataPath)) {
            return create(inputStream);
        }
    }

    public static EnmapMetadata create(InputStream inputStream) throws IOException {
        return new EnmapMetadata(createXmlDocument(inputStream));
    }

    public String getProductName() throws IOException {
        String fileName = getNodeContent("/level_X/metadata/name");
        return fileName.substring(0, 73);
    }

    public ProductData.UTC getStartTime() throws IOException, ParseException {
        String time = getNodeContent("/level_X/base/temporalCoverage/startTime");
        return parseTimeString(time);
    }

    public String getProductType() throws IOException {
        return getNodeContent("/level_X/base/format");
    }

    public Dimension getSceneDimension() throws IOException {
        int width = Integer.parseInt(getNodeContent("/level_X/specific/widthOfScene"));
        int height = Integer.parseInt(getNodeContent("/level_X/specific/heightOfScene"));
        return new Dimension(width, height);
    }

    /**
     * returns the sun elevation angles at the four corner points of the scene
     *
     * @return an array containing the sun elevation angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    public double[] getSunElevationAngles() throws IOException {
        return getAngles("/level_X/specific/sunElevationAngle/*");
    }

    public double getSunElevationAngleCenter() throws IOException {
        return getAngleCenter("sunElevationAngle");
    }

    public double[] getSunAzimuthAngles() throws IOException {
        return getAngles("/level_X/specific/sunAzimuthAngle/*");
    }

    public double getSunAzimuthAngleCenter() throws IOException {
        return getAngleCenter("sunAzimuthAngle");
    }

    public double[] getAcrossOffNadirAngles() throws IOException {
        return getAngles("/level_X/specific/acrossOffNadirAngle/*");
    }

    public double getAcrossOffNadirAngleCenter() throws IOException {
        return getAngleCenter("acrossOffNadirAngle");
    }

    public double[] getAlongOffNadirAngles() throws IOException {
        return getAngles("/level_X/specific/alongOffNadirAngle/*");
    }

    public double getAlongOffNadirAngleCenter() throws IOException {
        return getAngleCenter("alongOffNadirAngle");
    }

    public double[] getSceneAzimuthAngles() throws IOException {
        return getAngles("/level_X/specific/sceneAzimuthAngle/*");
    }

    public double getSceneAzimuthAngleCenter() throws IOException {
        return getAngleCenter("sceneAzimuthAngle");
    }

    private double[] getAngles(String path) throws IOException {
        NodeList nodeSet = getNodeSet(path);
        double[] angles = new double[4];
        for (int i = 0; i < angles.length; i++) {
            angles[i] = Double.parseDouble(nodeSet.item(i).getTextContent());
        }
        return angles;
    }

    private double getAngleCenter(String alongOffNadirAngle) throws IOException {
        return Double.parseDouble(getNodeContent("/level_X/specific/" + alongOffNadirAngle + "/center"));
    }

    public ProductData.UTC getStopTime() throws IOException {
        String time = getNodeContent("/level_X/base/temporalCoverage/stopTime");
        return parseTimeString(time);
    }

    // todo - this class is borrowed from ISO8601Converter class in SNAP 10 (snap-core)
    // todo - when using SNAP 10 this method should be replaced
    private static ProductData.UTC parseTimeString(String iso8601String) {
        final TemporalAccessor accessor = FORMATTER.parse(iso8601String);
        final ZonedDateTime time = ZonedDateTime.from(accessor);
        final Date date = Date.from(time.toInstant());
        return ProductData.UTC.create(date, time.get(ChronoField.MICRO_OF_SECOND));
    }

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(inputStream);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Cannot create document from manifest XML file.", e);
        }
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

}
