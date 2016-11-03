package com.ciena.logx.logfile.ra.puml;

import com.ciena.logx.LogXPropertiesImpl;

/**
 * Created by gbromfie on 11/2/16.
 */
public class PumlPropertiesImpl extends LogXPropertiesImpl implements PumlProperties {

    private String _pumlFilename = null;
    private String _pngFilename = null;

    @Override
    public String getPumlFilename() {
        return _pumlFilename;
    }

    @Override
    public void setPumlFilename(String filename) {
        _pumlFilename = filename;
    }

    @Override
    public String getPngFilename() {
        return _pngFilename;
    }

    @Override
    public void setPngFilename(String filename) {
        _pngFilename = filename;
    }
}
