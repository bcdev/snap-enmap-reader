package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.Assert.*;

public class EnmapProductReaderTest {

    private EnmapProductReader reader;
    private Path productPath;

    @Before
    public void setUp() throws Exception {
        reader = new EnmapProductReader(new EnmapProductReaderPlugIn());
        productPath = Paths.get(Objects.requireNonNull(EnmapMetadataTestUtils.class.getResource("enmap_L2A_gtif_qualification.zip")).toURI());
    }

    @Test
    public void readProduct() throws IOException {
        Product product = reader.readProductNodes(productPath, null);
        assertEquals("ENMAP01-____L2A-DT000326721_20170626T102020Z_001_V000204_20200406T201930Z", product.getName());
        assertEquals("ENMAP_L2A", product.getProductType());
        assertEquals(1000, product.getSceneRasterWidth());
        assertEquals(1024, product.getSceneRasterHeight());
        assertNotNull("GeoCoding should not be null", product.getSceneGeoCoding());
    }
}