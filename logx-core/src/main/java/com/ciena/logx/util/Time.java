package com.ciena.logx.util;

/**
 * Created by gbromfie on 5/10/16.
 */
public class Time {
    static public String msToString(long ms) {
        long value = ms;
        int numMins = (int)((double)value / (double)60000);
        value = value - (60000 * numMins);
        int numSecs = (int) ((double)value / (double)1000);
        int numMsecs = (int)(value - (1000 * numSecs));
        if (numMins > 0) {
            return String.format("%dm %ds %dms", numMins, numSecs, numMsecs);
        } else if (numSecs > 0) {
            return String.format("%ds %dms", numSecs, numMsecs);
        } else {
            return String.format("%dms", numMsecs);
        }
    }
}
