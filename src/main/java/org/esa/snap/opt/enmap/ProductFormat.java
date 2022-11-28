package org.esa.snap.opt.enmap;

public enum ProductFormat {

    BSQ_Metadata, // BSQ+Metadata
    BIL_Metadata, // BIL+Metadata
    BIP_Metadata, // BIP+Metadata
    JPEG2000_Metadata, // JPEG2000+Metadata
    GeoTIFF_Metadata; // GeoTIFF+Metadata

    public String asEnmapFormatName(){
        return name().replace("_", "+");
    }

    public static String toEnumName(String formatName){
        return formatName.replace("+", "_");
    }


}
