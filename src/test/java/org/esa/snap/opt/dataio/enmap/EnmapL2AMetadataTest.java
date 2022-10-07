package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EnmapL2AMetadataTest {

    private static EnmapMetadata meta;

    @BeforeClass
    public static void beforeClass() throws Exception {
        meta = EnmapMetadata.create(getMetaDataStream("ENMAP01-____L2A-DT000326721_20170626T102020Z_001_V000204_20200406T201930Z-METADATA.XML"));
    }

    @Test
    public void testBasicProductInfo() throws Exception {
        ProductData.UTC startTime = meta.getStartTime();
        Calendar startCal = startTime.getAsCalendar();
        Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // expected 2017-06-26T10:20:20.999936Z
        assertEquals(2017, startCal.get(Calendar.YEAR));
        assertEquals(5, startCal.get(Calendar.MONTH));
        assertEquals(26, startCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, startCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, startCal.get(Calendar.MINUTE));
        assertEquals(21, startCal.get(Calendar.SECOND));        // time is rounded from microseconds to milliseconds
        assertEquals(0, startCal.get(Calendar.MILLISECOND));    // time is rounded from microseconds to milliseconds
        assertEquals(TimeZone.getTimeZone("UTC"), startCal.getTimeZone());

        ProductData.UTC stopTime = meta.getStopTime();
        Calendar stopCal = stopTime.getAsCalendar();
        // expected 2017-06-26T10:20:25.545157Z
        assertEquals(2017, stopCal.get(Calendar.YEAR));
        assertEquals(5, stopCal.get(Calendar.MONTH));
        assertEquals(26, stopCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, stopCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, stopCal.get(Calendar.MINUTE));
        assertEquals(25, stopCal.get(Calendar.SECOND));
        assertEquals(545, stopCal.get(Calendar.MILLISECOND));
        assertEquals(TimeZone.getTimeZone("UTC"), stopCal.getTimeZone());

        assertEquals("ENMAP01-____L2A-DT000326721_20170626T102020Z_001_V000204_20200406T201930Z", meta.getProductName());
        assertEquals("ENMAP_L2A", meta.getProductType());
        assertEquals(new Dimension(1000, 1024), meta.getSceneDimension());
    }

    @Test
    public void testAngles() throws IOException {
        assertArrayEquals(new double[]{62.843017, 63.025996, 63.232013, 63.048052}, meta.getSunElevationAngles(), 1.0e-6f);
        assertEquals(63.038384f, meta.getSunElevationAngleCenter(), 1.0e-6);

        assertArrayEquals(new double[]{148.98072, 149.614311, 149.224083, 148.59262}, meta.getSunAzimuthAngles(), 1.0e-6f);
        assertEquals(149.106702, meta.getSunAzimuthAngleCenter(), 1.0e-6f);

        assertArrayEquals(new double[]{1.27641076292, -1.35358251158, -1.4507597324, 1.17928885285}, meta.getAcrossOffNadirAngles(), 1.0e-6f);
        assertEquals(-0.0871606570525, meta.getAcrossOffNadirAngleCenter(), 1.0e-6);

        assertArrayEquals(new double[]{-0.0692040225255, -0.0695301149423, -0.167785438271, -0.16870825914}, meta.getAlongOffNadirAngles(), 1.0e-6f);
        assertEquals(-0.11880695872, meta.getAlongOffNadirAngleCenter(), 1.0e-6);

        assertArrayEquals(new double[]{14.2906888082, 14.2906888082, 14.2149804324, 14.2149804324}, meta.getSceneAzimuthAngles(), 1.0e-6f);
        assertEquals(14.2528346203, meta.getSceneAzimuthAngleCenter(), 1.0e-6);
    }

    private static InputStream getMetaDataStream(String fileName) throws URISyntaxException, IOException {
        URI resource = EnmapL2AMetadataTest.class.getResource("ENMAP_L1B-L1C-L2A-METADATA.zip").toURI();
        ZipFile zip = new ZipFile(new File(resource));
        ZipEntry entry = zip.getEntry(fileName);
        return zip.getInputStream(entry);
    }

}