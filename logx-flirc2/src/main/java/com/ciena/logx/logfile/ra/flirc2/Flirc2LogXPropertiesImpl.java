package com.ciena.logx.logfile.ra.flirc2;

import com.ciena.logx.LogXPropertiesImpl;

/**
 * Created by gbromfie on 11/2/16.
 */
public class Flirc2LogXPropertiesImpl extends LogXPropertiesImpl implements Flirc2LogXProperties {

    private String _recordingFilename = null;

    @Override
    public String getRecordingFilename() {
        return _recordingFilename;
    }

    @Override
    public void setRecordingFilename(String filename) {
        _recordingFilename = filename;
    }
}
