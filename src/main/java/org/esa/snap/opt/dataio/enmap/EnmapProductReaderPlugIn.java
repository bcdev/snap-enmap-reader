package org.esa.snap.opt.dataio.enmap;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class EnmapProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String[] FILE_EXTENSIONS = {".zip", ".xml"};
    public static final String DESCRIPTION = "EnMAP L1B/L1C/L2A Product Reader";
    private String[] FORMAT_NAMES = new String[]{"EnMAP L1B/L1C/L2A"};


    @Override
    public DecodeQualification getDecodeQualification(Object o) {
        try {
            Path path = convertToPath(o);
            if (!isZip(path)) {
                path = path.getParent();
            }
            if (path != null) {
                VirtualDir virtualDir;
                virtualDir = VirtualDir.create(path.toFile());
                String[] fileNames = virtualDir.listAllFiles();
                if (areEnmapL1bFiles(fileNames) || areEnmapL1cFiles(fileNames) || areEnmapL2aFiles(fileNames)) {
                    return DecodeQualification.INTENDED;
                }
            }
        } catch (Throwable t) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.UNABLE;
    }

    private static boolean isZip(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith("zip");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new EnmapProductReader(this);
    }

    @Override
    public Class[] getInputTypes() {
        return InputTypes.getTypes();
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], FILE_EXTENSIONS, DESCRIPTION);
    }

    static Path convertToPath(final Object object) {
        try {
            return InputTypes.toPath(object);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean areEnmapL1bFiles(String[] fileNames) {
        // todo
        return false;
    }

    private boolean areEnmapL1cFiles(String[] fileNames) {
        // todo
        return false;
    }

    private boolean areEnmapL2aFiles(String[] fileNames) {
        return Arrays.stream(EnmapFileUtils.L2A_FILENAME_PATTERNS).allMatch(p ->
                Arrays.stream(fileNames).anyMatch(f -> p.matcher(f).matches()));
    }

}
