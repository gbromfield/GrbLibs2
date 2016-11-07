package com.ciena.logx.logfile.ra.flirc2;

import com.ciena.logx.LogXProperties;

/**
 * Created by gbromfie on 11/1/16.
 */
public interface Flirc2LogXProperties extends LogXProperties {
    public String getRecordingFilename();
    public void setRecordingFilename(String filename);
}
