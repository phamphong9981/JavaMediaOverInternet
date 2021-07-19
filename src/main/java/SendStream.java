import com.phong.project3.DeviceList;

import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import java.io.IOException;
import java.net.UnknownHostException;

public class SendStream {
    String iphost;
    String port;
    CaptureDeviceInfo microphone;
    Player speaker;

    public SendStream() {
    }

    public SendStream setIpHost(String ipHost){
        this.iphost=ipHost;
        return this;
    }

    public SendStream setPort(String port){
        this.port=port;
        return this;
    }
    DataSource dataOutput=null;
    DataSink rtptransmitter=null;
    Processor processor=null;
    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    Integer getStateLock() {
        return stateLock;
    }
    private void setFailed() {
        failed = true;
    }
    private class StateListener implements ControllerListener {

        public void controllerUpdate(ControllerEvent ce) {

            // If there was an error during configure or
            // realize, the processor will be closed
            if (ce instanceof ControllerClosedEvent)
                setFailed();

            // All controller events, send a notification
            // to the waiting thread in waitForState method.
            if (ce instanceof ControllerEvent) {
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        }
    }
    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new SendStream.StateListener());
        failed = false;

        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }

        //cho den khi nao state thuc su dc doi state
        while (p.getState() < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }

        if (failed)
            return false;
        else
            return true;
    }
    private String determindDataOutput(){
        DataSource dataSource=null;
        try {
            dataSource= Manager.createDataSource(microphone.getLocator());
            processor=Manager.createProcessor(dataSource);
            waitForState(processor,Processor.Configured);
            TrackControl[] trackControls=processor.getTrackControls();
            if (trackControls == null || trackControls.length < 1)
                return "Couldn't find trackControls in processor";
            boolean programmed = false;
            //Xu ly format
            AudioFormat afmt;
            for (int i = 0; i < trackControls.length; i++) {
                Format format = trackControls[i].getFormat();
                if (  trackControls[i].isEnabled() &&
                        format instanceof AudioFormat &&
                        !programmed) {
                    afmt = (AudioFormat)trackControls[i].getFormat();
                    AudioFormat ulawFormat =   new AudioFormat(AudioFormat.DVI_RTP);

                    trackControls[i].setFormat (ulawFormat);
                    System.err.println("Audio transmitted as:");
                    System.err.println("  " + ulawFormat);
                    // Assume succesful
                    programmed = true;
                } else
                    trackControls[i].setEnabled(false);
            }
            if (!programmed)
                return "Couldn't find Audio track";

            ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
            processor.setContentDescriptor(cd);
            waitForState(processor, Controller.Realized);
            dataOutput = processor.getDataOutput();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoDataSourceException e) {
            e.printStackTrace();
        } catch (NoProcessorException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void stop() {
        processor.stop();
        processor.close();
        processor = null;
        rtptransmitter.close();
        rtptransmitter = null;
    }

    public void start() throws UnknownHostException {
        microphone= DeviceList.getDeviceList().get(0);
        determindDataOutput();
        String rtpURL = "rtp://" + iphost + ":" + port + "/audio";
        MediaLocator outputLocator = new MediaLocator(rtpURL);

        try {
            rtptransmitter = Manager.createDataSink(dataOutput, outputLocator);
            rtptransmitter.open();
            rtptransmitter.start();
            dataOutput.start();
        } catch (MediaException me) {
            return ;
        } catch (IOException ioe) {
            return ;
        }
        processor.start();
    }

    public static void main(String[] args) throws UnknownHostException {
        new SendStream().setIpHost("192.168.111.2").setPort("8888").start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                new AVReceive2(new String[]{"8888"}).initialize();
            }
        }).start();
    }
}
