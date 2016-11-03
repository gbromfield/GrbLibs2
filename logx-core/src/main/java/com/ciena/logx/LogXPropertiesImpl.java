package com.ciena.logx;

import com.ciena.logx.logrecord.LogRecordParser;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.util.ExtensionFilter;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by gbromfie on 11/1/16.
 */
public class LogXPropertiesImpl implements LogXProperties {
    private ArrayList<String> _inputFilenames = null;
    private ArrayList<File> _inputFiles = null;
    private ExtensionFilter _filter = null;
    private ArrayList<String> _parserNames = null;
    private ArrayList<LogRecordParser> _parsers = null;
    private Date[] _dateRange = null;
    private HashSet<String> _incTids = null;
    private HashSet<String> _exclTids = null;
    private HashSet<String> _incSids = null;
    private HashSet<String> _exclSids = null;
    private Boolean _inclusive = null;
    private OutputContext _outputContext = null;
    private Boolean _printHelp = null;
    private String _unknownArg = null;

    @Override
    public ArrayList<String> getInputFilenames() {
        return _inputFilenames;
    }

    @Override
    public void addInputFilename(String inputFilename) {
        if (_inputFilenames == null) {
            _inputFilenames = new ArrayList<String>();
        }
        _inputFilenames.add(inputFilename);
    }

    @Override
    public ArrayList<File> getInputFiles() {
        if (_inputFiles == null) {
            if (_inputFilenames != null) {
                _inputFiles = LogX.processFilenames(_inputFilenames, _filter);
            }
        }
        return _inputFiles;
    }

    @Override
    public ExtensionFilter getFilter() {
        return _filter;
    }

    @Override
    public void setFilter(ExtensionFilter filter) {
        _filter = filter;
    }

    @Override
    public ArrayList<String> getParserNames() {
        return _parserNames;
    }

    @Override
    public void addParserName(String parserName) {
        if (_parserNames == null) {
            _parserNames = new ArrayList<String>();
        }
        _parserNames.add(parserName);
    }

    @Override
    public ArrayList<LogRecordParser> getParsers() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (_parsers == null) {
            if (_parserNames != null) {
                _parsers = new ArrayList<LogRecordParser>();
                for(String parserName : _parserNames) {
                    Class parserClass = Class.forName(parserName);
                    Constructor ctor = parserClass.getConstructor(OutputContext.class);
                    _parsers.add((LogRecordParser)ctor.newInstance(_outputContext));
                }
            }
        }
        return _parsers;
    }

    @Override
    public Date[] getDateRange() {
        return _dateRange;
    }

    @Override
    public void setDateRange(Date[] range) {
        if ((range != null) && (range.length != 2)) {
            throw new IllegalArgumentException("Date range must be an array of two dates, from and to");
        }
        _dateRange = range;
    }

    @Override
    public Date getFromDate() {
        if (_dateRange != null) {
            return _dateRange[0];
        }
        return null;
    }

    @Override
    public Date getToDate() {
        if (_dateRange != null) {
            return _dateRange[1];
        }
        return null;
    }

    @Override
    public HashSet<String> getIncTids() {
        return _incTids;
    }

    @Override
    public void setIncTids(HashSet<String> incTids) {
        _incTids = incTids;
    }

    @Override
    public HashSet<String> getExclTids() {
        return _exclTids;
    }

    @Override
    public void setExclTids(HashSet<String> exclTids) {
        _exclTids = exclTids;
    }

    @Override
    public HashSet<String> getIncSids() {
        return _incSids;
    }

    @Override
    public void setIncSids(HashSet<String> incSids) {
        _incSids = incSids;
    }

    @Override
    public HashSet<String> getExclSids() {
        return _exclSids;
    }

    @Override
    public void setExclSids(HashSet<String> exclSids) {
        _exclSids = exclSids;
    }

    @Override
    public Boolean getInclusive() {
        return _inclusive;
    }

    @Override
    public void setInclusive(boolean inclusive) {
        _inclusive = inclusive;
    }

    @Override
    public OutputContext getOutputContext() {
        return _outputContext;
    }

    @Override
    public void setOutputContext(OutputContext outputContext) {
        _outputContext = outputContext;
    }

    @Override
    public boolean printHelp() {
        if (_printHelp == null) {
            return false;
        }
        return _printHelp;
    }

    @Override
    public void setPrintHelp(boolean printHelp) {
        _printHelp = printHelp;
    }

    @Override
    public String getUnknownArg() {
        return _unknownArg;
    }

    @Override
    public void setUnknownArg(String unknownArg) {
        _unknownArg = unknownArg;
    }
}
