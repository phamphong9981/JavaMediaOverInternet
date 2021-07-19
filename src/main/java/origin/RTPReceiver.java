package origin;

import javax.media.*;
import java.io.IOException;
import java.net.MalformedURLException;

public class RTPReceiver{
    public static Player player = null;
    public static void main(String[] args) {
        String url = "rtp://192.168.1.204:8000/audio/1";

        MediaLocator mrl = new MediaLocator(url);

        // Create a player for this rtp session

        try
        {
            player = Manager.createPlayer(mrl);
        } catch (NoPlayerException e)
        {
            e.printStackTrace();
            System.exit(-1);
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        if (player != null)
        {
            System.out.println("Player created.");
            player.realize();
            player.addControllerListener(new ControllerListener() {
                @Override
                public void controllerUpdate(ControllerEvent controllerEvent) {
                    System.out.println(controllerEvent);
                    if(controllerEvent instanceof TransitionEvent) {
                        if (((TransitionEvent)controllerEvent).getCurrentState() == Processor.Realized) {
                            player.start();
                            System.out.println("starting player now");
                        }
                    }
                }
            });
            System.out.println("Starting player");
            player.start();
        } else
        {
            System.err.println("Player won't create.");
            System.exit(-1);
        }

        System.out.println("Exiting.");
    }
}
