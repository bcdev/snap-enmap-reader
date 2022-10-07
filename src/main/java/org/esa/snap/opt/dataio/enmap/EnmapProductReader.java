package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

class EnmapProductReader extends AbstractProductReader {
    private final Logger logger;

    public EnmapProductReader(EnmapProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        Path path = InputTypes.toPath(super.getInput());
        VirtualDir virtualDir = VirtualDir.create(path.toFile());
        String[] fileNames = virtualDir.listAllFiles();
        String metadataFile = getMetadataFile(fileNames);
        EnmapMetadata meta = EnmapMetadata.create(virtualDir.getInputStream(metadataFile));

        Dimension dimension = meta.getSceneDimension();
        Product product = new Product(meta.getProductName(), meta.getProductType(), dimension.width, dimension.height);
        // todo
        return product;
    }

    private String getMetadataFile(String[] fileNames) throws IOException {
        Optional<String> first = Arrays.stream(fileNames).filter(s -> s.endsWith(EnmapFileUtils.METADATA_SUFFIX)).findFirst();
        return first.orElseThrow(() -> new IOException("Metadata file not found"));
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }

    @Override
    public void readTiePointGridRasterData(TiePointGrid tpg, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        super.readTiePointGridRasterData(tpg, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }

    @Override
    public void close() throws IOException {

    }

}
