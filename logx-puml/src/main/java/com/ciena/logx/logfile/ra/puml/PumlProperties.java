package com.ciena.logx.logfile.ra.puml;

import com.ciena.logx.LogXProperties;

/**
 * Created by gbromfie on 11/2/16.
 */
public interface PumlProperties extends LogXProperties {
    public String getPumlFilename();
    public void setPumlFilename(String filename);

    public String getPngFilename();
    public void setPngFilename(String filename);
}
