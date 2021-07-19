package origin;

import jmapps.util.StateHelper;

import javax.media.*;
import javax.media.control.StreamWriterControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import java.io.IOException;
import java.util.Vector;

class JMFMicrophone {
    public static void main(String[] args) {
        CaptureDeviceInfo di=null;
        Processor p=null;
        StateHelper sh=null;
        Vector deviceList= CaptureDeviceManager.getDeviceList(null);
        if (deviceList.size()>0){
            di= (CaptureDeviceInfo) deviceList.get(0);
            System.out.println(deviceList.size());
        }else {
            System.exit(1);
        }
        try {
            p= Manager.createProcessor(di.getLocator());
            sh=new StateHelper(p);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoProcessorException e) {
            e.printStackTrace();
        }
        if (!sh.configure(1000)){
            System.exit(0);

        }
        p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
        if (!sh.realize(1000))
            System.exit(-1);
        DataSource source=p.getDataOutput();
        MediaLocator dest=new MediaLocator("file://D:/capture.wav");
        DataSink fileWriter=null;
        try {
            fileWriter=Manager.createDataSink(source,dest);
            fileWriter.open();
        } catch (NoDataSinkException | IOException e) {
            e.printStackTrace();
        }
//        StreamWriterControl swc=(StreamWriterControl) p.getControls("javax.media.control.StreamWriterControl");
//        if (swc!=null){
//            swc.setStreamSizeLimit(5000000);
//        }
        try {
            fileWriter.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sh.playToEndOfMedia(5000);
        sh.close();
        fileWriter.close();
    }
}
