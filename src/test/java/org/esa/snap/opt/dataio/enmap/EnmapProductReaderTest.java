package org.esa.snap.opt.dataio.enmap;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class EnmapProductReaderTest {

    private EnmapProductReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new EnmapProductReader(new EnmapProductReaderPlugIn());
    }

    @Test
    public void readProduct() throws IOException {
        reader.readProductNodes(null , null);
    }
}