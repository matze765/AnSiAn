package de.tu.darmstadt.seemoo.ansian.control.events;

import android.net.Uri;

/**
 * Created by MATZE on 22.02.2017.
 */

public class ImagePickIntentResultEvent {
    private Uri file;

    public ImagePickIntentResultEvent(Uri file) {
        this.file = file;
    }

    public Uri getFile() {
        return file;
    }

    public void setFile(Uri file) {
        this.file = file;
    }



}
