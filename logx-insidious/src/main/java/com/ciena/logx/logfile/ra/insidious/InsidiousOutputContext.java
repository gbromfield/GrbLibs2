package com.ciena.logx.logfile.ra.insidious;

import com.ciena.logx.LogX;
import com.ciena.logx.LogXProperties;
import com.ciena.logx.logfile.ra.insidious.logrecord.TL1LogRecordParser;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.ciena.logx.output.OutputRecordSet;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by gbromfie on 9/2/16.
 */
public class InsidiousOutputContext implements OutputContext {
    final static public SimpleDateFormat DateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS");

    private InsidiousLogXProperties _props;
    private File _capOutputFile;
    private PrintWriter _capOutputWriter;
    private OutputRecordSet _logItems;
    private HashMap<String, String> _mapper;
    private HashMap<String, Object> _statsMap;
    private String _buffer;

    public InsidiousOutputContext(InsidiousLogXProperties props) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        _props = props;
        if (_props.getRecordingFilename() == null) {
            throw new IllegalArgumentException("Error: No output recording file specified, must specify -cap");
        }
        _props.addParserName(TL1LogRecordParser.class.getName());   // add tL1 by default
        _props.setOutputContext(this);
        _mapper = new HashMap<String, String>();
        _statsMap = new HashMap<String, Object>();
        _buffer = null;
        _logItems = new OutputRecordSet(_props.getFromDate(), _props.getToDate());
        if (_props.getInclusive() == null) {
            _props.setInclusive(false);
        }
    }

    @Override
    public LogXProperties getProperties() {
        return _props;
    }

    public boolean passesFilter(String ncid) {
        if (ncid == null) {
            return true;
        }
        String ncidNo_ = ncid.substring(1, ncid.length()-1);
        if (_props.getIncSids() != null) {
            return _props.getIncSids().contains(ncidNo_);
        }
        if (_props.getExclSids() != null) {
            return !_props.getExclSids().contains(ncidNo_);
        }
        String tid = _mapper.get(ncid);
        if (tid == null) {
            if (_props.getInclusive()) {
                return false;
            } else {
                return true;
            }
        }
        if (_props.getIncTids() != null) {
            return _props.getIncTids().contains(tid);
        }
        if (_props.getExclTids() != null) {
            return !_props.getExclTids().contains(tid);
        }
        if (_props.getInclusive()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void init() throws Exception {
        if (_props.getRecordingFilename() != null) {
            _capOutputFile = new File(_props.getRecordingFilename());
            if (_capOutputFile.exists()) {
                _capOutputFile.delete();
            }
            _capOutputWriter = new PrintWriter(new BufferedWriter(new FileWriter(_capOutputFile)));
        }
        _logItems.addFirst("{\n\"recording\": [\n");
    }

    public OutputRecordSet getOutputRecordSet() { return _logItems; }

    @Override
    public void put(String key, String value) {
        _mapper.put(key, value);
    }

    @Override
    public String get(String key) {
        return _mapper.get(key);
    }

    @Override
    public int incIntStat(String key, int value) {
        Integer statVal = (Integer)_statsMap.get(key);
        if (statVal == null) {
            _statsMap.put(key, value);
            return 0;
        } else {
            _statsMap.put(key, statVal.intValue() + value);
            return statVal.intValue();
        }
    }

    @Override
    public Map<String, Object> getStats() {return _statsMap; }

    @Override
    public void close() {
        _logItems.addLast("]}");

        if (_props.getRecordingFilename() != null) {
            _capOutputWriter.print(logItemsToString());
            _capOutputWriter.flush();
            _capOutputWriter.close();
        }
    }

    @Override
    public int size() { return _logItems.size(); }

    @Override
    public void onFinally() {
        if ((_props.getRecordingFilename() != null) && (_capOutputWriter != null)) {
            _capOutputWriter.close();
        }
    }

    @Override
    public String logItemsToString() {
        if (_buffer == null) {
            StringBuilder bldr = new StringBuilder();
            int size = _logItems.size();
            int filteredOut = 0;
            for(int i = 0; i < size; i++) {
                OutputRecord rec = _logItems.get(i);
                if (passesFilter((String)rec.getEnvValue("ncid"))) {
                    bldr.append(rec.getLogString());
                } else {
                    filteredOut++;
                }
            }
            System.out.println(String.format("Filtered out %d rows", filteredOut));
            for(String key : _mapper.keySet()) {
                String value = _mapper.get(key);
                int index = 0;
                while((index = bldr.indexOf(key)) != -1) {
                    bldr.replace(index, index + key.length(), value);
                }
            }
            int index = bldr.lastIndexOf(",");
            if (index >= 0) {
                bldr.deleteCharAt(index);
            }
            _buffer = bldr.toString();
        }
        return _buffer;
    }

    public static void main(String[] args) {
        InsidiousLogXCommandLineProcessor clp = new InsidiousLogXCommandLineProcessor();
        InsidiousLogXProperties props = (InsidiousLogXProperties)clp.parse(args);

        if (props.getUnknownArg() != null) {
            System.out.println(String.format("Unknown argument: \"%s\"", props.getUnknownArg()));
            System.out.println("Syntax:");
            System.out.println(clp.getSyntax());
            System.exit(0);
        }

        if (props.printHelp()) {
            System.out.println("Syntax:");
            System.out.println(clp.getSyntax());
            System.exit(0);
        }

        if ((props.getInputFilenames() == null) || (props.getInputFilenames().size() == 0)) {
            System.out.println("Error: No input file specified");
            System.out.println(clp.getSyntax());
            System.exit(1);
        }

        InsidiousOutputContext ctx = null;
        try {
            ctx = new InsidiousOutputContext(props);
            LogX logx = new LogX(props);
            logx.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
