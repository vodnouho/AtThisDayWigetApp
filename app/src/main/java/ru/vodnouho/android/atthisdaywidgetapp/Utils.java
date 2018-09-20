package ru.vodnouho.android.atthisdaywidgetapp;

/**
 * Created by petukhov on 07.04.2017.
 */

public class Utils {

    public static int getTransparency(int rgb){
        int result = rgb & 0xFF000000;
        return result >>> 24;
    }

    /**
     *
     * @param transparency in range 0x00-0xFF
     * @param rgb - color
     * @return
     */
    public static int setTransparency(int transparency, int rgb){
        transparency = transparency << 24;
        rgb = rgb & 0x00FFFFFF;
        return transparency | rgb;
    }

}
