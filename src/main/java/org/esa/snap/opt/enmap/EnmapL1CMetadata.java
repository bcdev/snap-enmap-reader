package org.esa.snap.opt.enmap;

import org.esa.snap.core.datamodel.ProductData;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.io.IOException;

class EnmapL1CMetadata extends EnmapOrthoMetadata {

    EnmapL1CMetadata(Document doc, XPath xPath) {
        super(doc, xPath);
    }

    @Override
    public String getSpectralBandDescription(int index) throws IOException {
        return String.format("Sensor radiance @%s", getCentralWavelength(index));
    }

    @Override
    public String getSpectralUnit() {
        return "W/m^2/sr/nm";
    }

    @Override
    public int getSpectralDataType() {
        return ProductData.TYPE_UINT16;
    }

}
