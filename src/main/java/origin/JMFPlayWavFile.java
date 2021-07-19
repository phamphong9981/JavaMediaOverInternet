package origin;

import javax.media.*;
import java.io.IOException;
import java.net.URL;

public class JMFPlayWavFile {
    public static void main(String[] args) {
        try {
            Player player=Manager.createPlayer(new URL("file",null,"G:/haha.wav"));
            System.out.println(player.getState());
            player.realize();
            player.addControllerListener(new ControllerListener() {
                @Override
                public void controllerUpdate(ControllerEvent controllerEvent) {
                    if (controllerEvent instanceof EndOfMediaEvent){
                        player.stop();
                        player.close();
                    }
                }
            });
            player.start();
            System.out.println(player.getState());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoPlayerException e) {
            e.printStackTrace();
        }
    }
}
