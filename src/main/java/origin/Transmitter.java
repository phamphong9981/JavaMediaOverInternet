package origin;

import com.phong.project3.DeviceList;

import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import java.io.IOException;

public class Transmitter {
    private MediaLocator locator;
    private String ipAddress;
    private String port;

    private Processor processor = null;
    private DataSink rtptransmitter = null;
    private DataSource dataOutput = null;

    public Transmitter(MediaLocator locator,  String ipAddress, String port){
        this.locator = locator;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public synchronized String start() {
        String result;

        result = createProcessor();
        if (result != null)
            return result;

        result = createTransmitter();
        if (result != null) {
            processor.close();
            processor = null;
            return result;
        }

        processor.start();

        return null;
    }

    public void stop() {
        processor.stop();
        processor.close();
        processor = null;
        rtptransmitter.close();
        rtptransmitter = null;
    }
    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    Integer getStateLock() {
        return stateLock;
    }

    void setFailed() {
        failed = true;
    }
    class StateListener implements ControllerListener {

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
        p.addControllerListener(new StateListener());
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

    private String createProcessor() {
        if (locator == null)
            return "Locator is null";

        DataSource ds;
        DataSource clone;

        try {
            ds = Manager.createDataSource(locator);
        } catch (Exception e) {
            return "Couldn't create DataSource";
        }

        try {
            processor = Manager.createProcessor(ds);
        } catch (NoProcessorException npe) {
            return "Couldn't create processor";
        } catch (IOException ioe) {
            return "IOException creating processor";
        }

        boolean result = waitForState(processor, Processor.Configured);
        if (result == false)
            return "Couldn't configure processor";

        TrackControl [] tracks = processor.getTrackControls();

        if (tracks == null || tracks.length < 1)
            return "Couldn't find tracks in processor";

        boolean programmed = false;
        AudioFormat afmt;

        for (int i = 0; i < tracks.length; i++) {
            Format format = tracks[i].getFormat();
            if (  tracks[i].isEnabled() &&
                    format instanceof AudioFormat &&
                    !programmed) {
                afmt = (AudioFormat)tracks[i].getFormat();
                AudioFormat ulawFormat =   new AudioFormat(AudioFormat.DVI_RTP);

                tracks[i].setFormat (ulawFormat);
                System.err.println("Audio transmitted as:");
                System.err.println("  " + ulawFormat);
                // Assume succesful
                programmed = true;
            } else
                tracks[i].setEnabled(false);
        }

        if (!programmed)
            return "Couldn't find Audio track";

        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);

        result = waitForState(processor, Controller.Realized);
        if (result == false)
            return "Couldn't realize processor";


        dataOutput = processor.getDataOutput();
        return null;
    }

    private String createTransmitter() {

        String rtpURL = "rtp://" + ipAddress + ":" + port + "/audio";
        MediaLocator outputLocator = new MediaLocator(rtpURL);


        try {
            rtptransmitter = Manager.createDataSink(dataOutput, outputLocator);
            rtptransmitter.open();
            rtptransmitter.start();
            dataOutput.start();
        } catch (MediaException me) {
            return "Couldn't create RTP data sink";
        } catch (IOException ioe) {
            return "Couldn't create RTP data sink";
        }

        return null;
    }


    public static void main(String [] args) {
        CaptureDeviceInfo microphone= DeviceList.getDeviceList().get(0);
        Transmitter transmitter = new Transmitter(microphone.getLocator(),
                args[0],
                args[1]);
        String result = transmitter.start();

        if (result != null) {
            System.err.println("Error : " + result);
            System.exit(0);
        }

        System.err.println("Start transmission for 60 seconds...");

        try {
            Thread.currentThread().sleep(60000);
        } catch (InterruptedException ie) {
        }

        // Stop the transmission
        transmitter.stop();

        System.err.println("...transmission ended.");

        System.exit(0);
    }
}
