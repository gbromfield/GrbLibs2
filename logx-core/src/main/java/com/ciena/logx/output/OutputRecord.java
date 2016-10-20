package com.ciena.logx.output;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by gbromfie on 7/22/16.
 */
public class OutputRecord {
    private Date _logDate;
    private Class<?> _logClass;
    private String _logType;
    private String _logString;
    private HashMap<String, Object> _env;

    public OutputRecord(Date logDate, Class<?> logClass, String logType, String format, Object ... args) {
        _logDate = logDate;
        _logClass = logClass;
        _logType = logType;
        if (format == null) {
            _logString = null;
        } else {
            _logString = String.format(format, args);
        }
        _env = null;
    }

    public Date getLogDate() {
        return _logDate;
    }

    public Class<?> getLogClass() {
        return _logClass;
    }

    public String getLogType() {
        return _logType;
    }

    public String getLogString() {
        return _logString;
    }

    public void setLogDate(Date logDate) {
        this._logDate = logDate;
    }

    public void setLogClass(Class<?> logClass) {
        this._logClass = logClass;
    }

    public void setLogType(String logType) {
        this._logType = logType;
    }

    public void setLogString(String logString) {
        this._logString = logString;
    }

    public void putEnvValue(String key, Object value) {
        if (_env == null) {
            _env = new HashMap<String, Object>();
        }
        _env.put(key, value);
    }

    public Object getEnvValue(String key) {
        if (_env == null) {
            return null;
        }
        return _env.get(key);
    }

    @Override
    public String toString() {
        return _logString;
    }
}
