package de.tu.darmstadt.seemoo.ansian.control.events.tx.image.sstv;

import java.io.File;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.modulation.SSTV;

/**
 * Created by MATZE on 25.02.2017.
 */

public class SSTVTransmitEvent extends TransmitEvent{
    private File image;
    private boolean crop;
    private boolean repeat;
    private SSTV.SSTV_TYPE type;

    /**
     *
     * @param state the requested state
     * @param sender the sender ( GUI, TX, TXCHAIN)
     * @param image the image to send
     * @param crop true, if image should be cropped to required resolution. false, if it should be scaled
     * @param repeat true, if the transmission should be repeated until it is aborted
     * @param type SSTV type
     */
    public SSTVTransmitEvent(State state, Sender sender, File image, boolean crop, boolean repeat, SSTV.SSTV_TYPE type){
        super(state, sender);
        this.image = image;
        this.crop = crop;
        this.repeat = repeat;
        this.type = type;
    }


}
