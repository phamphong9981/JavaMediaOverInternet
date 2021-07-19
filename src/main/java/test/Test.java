package test;

import com.phong.project3.DeviceList;
import com.sun.media.rtp.RTPSessionMgr;

import javax.media.Manager;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionManager;
import java.io.IOException;
import java.net.InetAddress;

public class Test {
    public static void main(String[] args) {
        System.out.println(DeviceList.getDeviceList());
    }
}
