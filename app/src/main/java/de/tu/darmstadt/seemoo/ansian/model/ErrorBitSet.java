package de.tu.darmstadt.seemoo.ansian.model;

import java.util.BitSet;

import android.util.Log;

public class ErrorBitSet extends BitSet {

    private int index;
    private boolean filled;
    private int size;

    public ErrorBitSet(int size) {
        super(size);
        this.filled = false;
        this.index = 0;
        this.size = size;
    }

    public void setBit(boolean b) {
        if (this.index == this.size) {
            this.index = 0;
            this.filled = true;
        }
        set(this.index, b);
        this.index++;
    }

    public float getSuccessRate() {
        if (this.filled)
            return (float) cardinality() / this.size;
        else
            return (float) cardinality() / this.index;
    }

    public float getErrorRate() {
        return 1f - getSuccessRate();
    }

    public boolean needsReinit(float threshold) {
        if (this.filled) {
            return getSuccessRate() < threshold;
        }
        return false;
    }

}
