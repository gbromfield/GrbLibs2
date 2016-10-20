package com.ciena.logx.logrecord;

import com.ciena.logx.output.OutputContext;

import java.io.PrintWriter;
import java.util.List;

/**
 * Created by gbromfie on 5/6/16.
 */
public interface LogRecordParser {
    public boolean parse(LogRecord logRecord);
    public void writeLegend();
    public void conflate();
}
