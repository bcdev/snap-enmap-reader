package org.esa.snap.opt.enmap;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.runtime.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

public class EnmapProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String[] FILE_EXTENSIONS = {".zip", ".xml"};
    public static final String DESCRIPTION = "EnMAP L1B/L1C/L2A Product Reader";
    public static final Preferences PREFERENCES = Config.instance("enmap").load().preferences();
    private final String[] FORMAT_NAMES = new String[]{"EnMAP L1B/L1C/L2A"};

    public static final String ENMAP_GEOTIFF_USE_JAI = "enmap.geotiff.useJai";

    static {
        EnMapRgbProfiles.registerRGBProfiles();
    }

    @Override
    public DecodeQualification getDecodeQualification(Object o) {
        try {
            Path path = convertToPath(o);
            if (path == null) {
                return DecodeQualification.UNABLE;
            }
            if (!EnmapFileUtils.isZip(path)) {
                path = path.getParent();
            }
            if (path != null) {
                VirtualDir virtualDir = VirtualDir.create(path.toFile());
                String[] fileNames = virtualDir.listAllFiles();
                List<Path> filePaths = new ArrayList<>();
                for (String fileName : fileNames) {
                    filePaths.add(Paths.get(fileName));
                }
                if (areEnmapL1bFiles(filePaths) || areEnmapL1cFiles(filePaths) || areEnmapL2aFiles(filePaths)) {
                    return DecodeQualification.INTENDED;
                }
            }
        } catch (Throwable t) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new EnmapProductReader(this);
    }

    @Override
    public Class<?>[] getInputTypes() {
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

    private boolean areEnmapL1bFiles(List<Path> filePaths) {
        return Arrays.stream(EnmapFileUtils.L1B_FILENAME_PATTERNS).allMatch(p ->
                filePaths.stream().anyMatch(path -> p.matcher(path.getFileName().toString()).matches()));
    }

    private boolean areEnmapL1cFiles(List<Path> filePaths) {
        return Arrays.stream(EnmapFileUtils.L1C_FILENAME_PATTERNS).allMatch(p ->
                filePaths.stream().anyMatch(path -> p.matcher(path.getFileName().toString()).matches()));
    }

    private boolean areEnmapL2aFiles(List<Path> filePaths) {
        return Arrays.stream(EnmapFileUtils.L2A_FILENAME_PATTERNS).allMatch(p ->
                filePaths.stream().anyMatch(path -> p.matcher(path.getFileName().toString()).matches()));
    }

}
