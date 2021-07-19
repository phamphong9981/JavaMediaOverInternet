package com.phong.project3;

import jmapps.util.StateHelper;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Processor;
import java.util.Vector;

public class DeviceList {
    private static CaptureDeviceInfo di=null;
    public static Vector<CaptureDeviceInfo> getDeviceList(){
        Vector deviceList= CaptureDeviceManager.getDeviceList(null);
        return deviceList;
    }
}
