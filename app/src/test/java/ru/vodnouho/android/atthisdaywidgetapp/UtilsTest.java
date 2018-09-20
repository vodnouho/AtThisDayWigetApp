package ru.vodnouho.android.atthisdaywidgetapp;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

    public void testGetTransparency() {
        int color = 0xFE001122;
        int expectedTransparency = 0xFE;
        assertEquals(expectedTransparency, Utils.getTransparency(color));
    }

    public void testSetTransparency() {
        int color = 0xFE001122;
        String s = Integer.toHexString(color);
        int transparency = 0x33;
        int expectedColor = 0x33001122;
        assertEquals(expectedColor, Utils.setTransparency(transparency, color));
    }
}