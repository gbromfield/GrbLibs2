package com.ciena.logx.logfile;

import com.ciena.logx.logrecord.LogRecord;

import java.util.Iterator;

/**
 * Created by gbromfie on 5/9/16.
 */
public interface LogFileParser extends Iterator<LogRecord> {
    public void dispose();
}
