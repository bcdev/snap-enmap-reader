package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.ProductData;
import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.Area;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

public interface EnmapMetadata {
    String NOT_AVAILABLE = "NA";

    static EnmapMetadata create(Path metadataPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(metadataPath)) {
            return create(inputStream);
        }
    }

    static EnmapMetadata create(InputStream inputStream) throws IOException {
        return new EnmapMetadataImpl(EnmapMetadata.createXmlDocument(inputStream));
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

    /**
     * The version of the XML schema.
     *
     * @return the version
     * @throws IOException in case the metadata XML file could not be read
     */
    String getSchemaVersion() throws IOException;

    /**
     * The version of the processing chain.
     *
     * @return the version
     * @throws IOException in case the metadata XML file could not be read
     */
    String getProcessingVersion() throws IOException;

    /**
     * The version of the processor used to generate the archived L0 product.
     *
     * @return the version
     * @throws IOException in case the metadata XML file could not be read
     */
    String getL0ProcessingVersion() throws IOException;

    /**
     * The name of the product
     *
     * @return the name
     * @throws IOException in case the metadata XML file could not be read
     */
    String getProductName() throws IOException;

    /**
     * The type string of the product
     *
     * @return the product type
     * @throws IOException in case the metadata XML file could not be read
     */
    String getProductType() throws IOException;

    /**
     * The data format of the product contents.
     * Is one of the following values {@code [BSQ+Metadata, BIL+Metadata, BIP+Metadata, JPEG2000+Metadata, GeoTIFF+Metadata]}
     *
     * @return the data format
     * @throws IOException in case the metadata XML file could not be read
     */
    String getProductFormat() throws IOException;

    /**
     * The sensing start time of the scene at UTC time zone
     *
     * @return the sensing start time
     * @throws IOException in case the metadata XML file could not be read
     */
    ProductData.UTC getStartTime() throws IOException, ParseException;

    /**
     * The sensing stop time of the scene at UTC time zone
     *
     * @return the sensing stop time
     * @throws IOException in case the metadata XML file could not be read
     */
    ProductData.UTC getStopTime() throws IOException;

    /**
     * The widht and height of the scene as dimension object
     *
     * @return the dimension of the scene
     * @throws IOException in case the metadata XML file could not be read
     */
    Dimension getSceneDimension() throws IOException;

    /**
     * Returns the spatial coverage of the scene raster in WGS84 coordinates for the satellite raster (Level L1B)
     *
     * @return the spatial coverage
     * @throws IOException in case the metadata XML file could not be read
     */
    Geometry getSpatialCoverage() throws IOException;

    /**
     * Returns the spatial coverage of the reprojected and orthorectified scene (Level L1C and L2A) in WGS84 coordinates,
     * regardless of the coordinate reference system.
     *
     * @return the spatial coverage
     * @throws IOException in case the metadata XML file could not be read
     */
    Geometry getSpatialOrthoCoverage() throws IOException;

    /**
     * Returns the geo-referencing information of the reprojected and orthorectified scene (Level L1C and L2A)
     *
     * @throws IOException in case the metadata XML file could not be read
     */
    GeoReferencing getGeoReferencing() throws IOException;

    /**
     * The sun elevation angles at the four corner points of the scene
     *
     * @return an array containing the sun elevation angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    double[] getSunElevationAngles() throws IOException;

    /**
     * The sun elevation angle at the center of the scene
     *
     * @return the sun elevation angle
     * @throws IOException in case the metadata XML file could not be read
     */
    double getSunElevationAngleCenter() throws IOException;

    /**
     * the sun azimuth angles at the four corner points of the scene
     *
     * @return an array containing the sun azimuth angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    double[] getSunAzimuthAngles() throws IOException;

    /**
     * The sun azimuth angle at the center of the scene
     *
     * @return the sun azimuth angle
     * @throws IOException in case the metadata XML file could not be read
     */
    double getSunAzimuthAngleCenter() throws IOException;

    /**
     * The across off-nadir angles at the four corner points of the scene
     *
     * @return an array containing the across off-nadir angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    double[] getAcrossOffNadirAngles() throws IOException;

    /**
     * The across off-nadir angle at the center of the scene
     *
     * @return the across off-nadir angle
     * @throws IOException in case the metadata XML file could not be read
     */
    double getAcrossOffNadirAngleCenter() throws IOException;

    /**
     * The along off-nadir angles at the four corner points of the scene
     *
     * @return an array containing the along off-nadir angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    double[] getAlongOffNadirAngles() throws IOException;

    /**
     * The along off-nadir angle at the center of the scene
     *
     * @return the along off-nadir angle
     * @throws IOException in case the metadata XML file could not be read
     */
    double getAlongOffNadirAngleCenter() throws IOException;

    /**
     * The scene azimuth angles at the four corner points of the scene
     *
     * @return an array containing the scene azimuth angles at the four corner points
     * @throws IOException in case the metadata XML file could not be read
     */
    double[] getSceneAzimuthAngles() throws IOException;

    /**
     * The scene azimuth angle at the center of the scene
     *
     * @return the scene azimuth angle
     * @throws IOException in case the metadata XML file could not be read
     */
    double getSceneAzimuthAngleCenter() throws IOException;

}
