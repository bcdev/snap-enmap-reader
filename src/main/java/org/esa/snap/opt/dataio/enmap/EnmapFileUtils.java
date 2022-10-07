package org.esa.snap.opt.dataio.enmap;

import java.util.regex.Pattern;

public class EnmapFileUtils {
    private static final String L1B_BASEFILENAME = "ENMAP\\d{2}-____L1B-DT.{9}_\\d{8}T\\d{6}Z_.{3}_V.{6}_\\d{8}T\\d{6}Z";
    private static final String L1C_BASEFILENAME = "ENMAP\\d{2}-____L1C-DT.{9}_\\d{8}T\\d{6}Z_.{3}_V.{6}_\\d{8}T\\d{6}Z";
    private static final String L2A_BASEFILENAME = "ENMAP\\d{2}-____L2A-DT.{9}_\\d{8}T\\d{6}Z_.{3}_V.{6}_\\d{8}T\\d{6}Z";

    public static final String METADATA_SUFFIX = "-METADATA.XML";
    static final Pattern[] L1B_FILENAME_PATTERNS = new Pattern[]{
            Pattern.compile(L1B_BASEFILENAME + METADATA_SUFFIX),
            Pattern.compile(L1B_BASEFILENAME + "-QL_PIXELMASK_SWIR.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_PIXELMASK_VNIR.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_CIRRUS.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_CLASSES.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_CLOUD.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_CLOUDSHADOW.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_HAZE.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_SNOW.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_TESTFLAGS_SWIR.TIF"),
            Pattern.compile(L1B_BASEFILENAME + "-QL_QUALITY_TESTFLAGS_VNIR.TIF"),
//            Pattern.compile(L1B_BASEFILENAME + "-QL_SWIR.TIF"), // not considering the RGB quicklook images
//            Pattern.compile(L1B_BASEFILENAME + "-QL_VNIR.TIF"), // not considering the RGB quicklook images
            Pattern.compile(L1B_BASEFILENAME + "-SPECTRAL_IMAGE_SWIR.(TIF|HDR|JPEG2000)"),
            Pattern.compile(L1B_BASEFILENAME + "-SPECTRAL_IMAGE_VNIR.(TIF|HDR|JPEG2000)"),
//            Pattern.compile(L1B_BASEFILENAME + "-SPECTRAL_IMAGE_SWIR.(BSQ|BIP|BIL)") // only in case of HDR
//            Pattern.compile(L1B_BASEFILENAME + "-SPECTRAL_IMAGE_VNIR.(BSQ|BIP|BIL)") // only in case of HDR
    };
    
    static final Pattern[] L1C_FILENAME_PATTERNS = new Pattern[]{
            Pattern.compile(L1C_BASEFILENAME + METADATA_SUFFIX),
            Pattern.compile(L1C_BASEFILENAME + "-QL_PIXELMASK.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_CIRRUS.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_CLASSES.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_CLOUD.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_CLOUDSHADOW.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_HAZE.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_SNOW.TIF"),
            Pattern.compile(L1C_BASEFILENAME + "-QL_QUALITY_TESTFLAGS.TIF"),
//            Pattern.compile(L1C_BASEFILENAME + "-QL_SWIR.TIF"), // not considering the RGB quicklook images
//            Pattern.compile(L1C_BASEFILENAME + "-QL_VNIR.TIF"), // not considering the RGB quicklook images
            Pattern.compile(L1C_BASEFILENAME + "-SPECTRAL_IMAGE.(TIF|HDR|JPEG2000)"),
//            Pattern.compile(L1C_BASEFILENAME + "-SPECTRAL_IMAGE.(BSQ|BIP|BIL)") // only in case of HDR
    };
    static final Pattern[] L2A_FILENAME_PATTERNS = new Pattern[]{
            Pattern.compile(L2A_BASEFILENAME + METADATA_SUFFIX),
            Pattern.compile(L2A_BASEFILENAME + "-QL_PIXELMASK.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_CIRRUS.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_CLASSES.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_CLOUD.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_CLOUDSHADOW.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_HAZE.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_SNOW.TIF"),
            Pattern.compile(L2A_BASEFILENAME + "-QL_QUALITY_TESTFLAGS.TIF"),
//            Pattern.compile(L2A_BASEFILENAME + "-QL_SWIR.TIF"), // not considering the RGB quicklook images
//            Pattern.compile(L2A_BASEFILENAME + "-QL_VNIR.TIF"), // not considering the RGB quicklook images
            Pattern.compile(L2A_BASEFILENAME + "-SPECTRAL_IMAGE.(TIF|HDR|JPEG2000)"),
//            Pattern.compile(L2A_BASEFILENAME + "-SPECTRAL_IMAGE.(BSQ|BIP|BIL)") // only in case of HDR
    };
}
