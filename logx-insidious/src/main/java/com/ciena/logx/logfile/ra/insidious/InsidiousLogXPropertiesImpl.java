package com.ciena.logx.logfile.ra.insidious;

import com.ciena.logx.LogXPropertiesImpl;

/**
 * Created by gbromfie on 11/2/16.
 */
public class InsidiousLogXPropertiesImpl extends LogXPropertiesImpl implements InsidiousLogXProperties {

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
