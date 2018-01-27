package de.android;


import de.android.drawer.SimpleMenu;

public class Config {
    public static String CONFIG_URL = "";
    public static final boolean HIDE_DRAWER = false;
    public static boolean USE_HARDCODED_CONFIG = false;
    public static final int INTERSTITIAL_INTERVAL = 5;

    public static void configureMenu(SimpleMenu menu, ConfigParser.CallBack callback){
        callback.configLoaded(false);
    }
}
