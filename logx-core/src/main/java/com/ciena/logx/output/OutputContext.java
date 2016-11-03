package com.ciena.logx.output;

import com.ciena.logx.LogXProperties;
import com.ciena.logx.logrecord.LogRecordParser;

import java.util.Date;
import java.util.Map;

/**
 * Created by gbromfie on 5/9/16.
 */
public interface OutputContext {
    public LogXProperties getProperties();
    public void init() throws Exception;
    public OutputRecordSet getOutputRecordSet();
    public void put(String key, String value);
    public String get(String key);
    public int incIntStat(String key, int value);
    public Map<String, Object> getStats();
    public void close() throws Exception;
    public int size();
    public void onFinally();
    public String logItemsToString();
}
