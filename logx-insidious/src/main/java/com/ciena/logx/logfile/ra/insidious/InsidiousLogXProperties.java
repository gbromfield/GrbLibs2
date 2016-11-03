package com.ciena.logx.logfile.ra.insidious;

import com.ciena.logx.LogXProperties;

/**
 * Created by gbromfie on 11/1/16.
 */
public interface InsidiousLogXProperties extends LogXProperties {
    public String getRecordingFilename();
    public void setRecordingFilename(String filename);
}
