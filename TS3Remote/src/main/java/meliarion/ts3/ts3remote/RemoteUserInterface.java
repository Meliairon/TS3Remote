package meliarion.ts3.ts3remote;

import android.os.Handler;

/**
 * Created by Meliarion on 06/07/13.
 * Interface that abstracts away the display server activity
 */
public interface RemoteUserInterface {

    void Received(String data);

    Handler getHandler();
}
