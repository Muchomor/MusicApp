package lab4zimny.pwr.musicapp;

import android.content.Context;
import android.widget.MediaController;


public class MusicController extends MediaController {
    Context context;
    public MusicController(Context context) {
        super(context);
        this.context=context;
    }
}
