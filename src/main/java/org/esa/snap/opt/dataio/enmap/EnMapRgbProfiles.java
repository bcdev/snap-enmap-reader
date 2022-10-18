package org.esa.snap.opt.dataio.enmap;

import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

class EnMapRgbProfiles {
    private EnMapRgbProfiles(){}

    static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("EnMAP VNIR",
                new String[]{
                        "band_71",
                        "band_47",
                        "band_26"
                },
                new String[]{
                        "ENMAP*",
                        "ENMAP_L*",
                        "",
                }
        ));
        manager.addProfile(new RGBImageProfile("EnMAP SWIR",
                new String[]{
                        "band_188",
                        "band_146",
                        "band_103"
                },
                new String[]{
                        "ENMAP*",
                        "ENMAP_L*",
                        "",
                }
        ));
    }
}
