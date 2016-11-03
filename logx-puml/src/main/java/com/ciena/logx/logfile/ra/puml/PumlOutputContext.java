package com.ciena.logx.logfile.ra.puml;

import com.ciena.logx.LogX;
import com.ciena.logx.LogXProperties;
import com.ciena.logx.logfile.ra.puml.logrecord.*;
import com.ciena.logx.logrecord.LogRecordParser;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.ciena.logx.output.OutputRecordSet;
import com.ciena.logx.util.ExtensionFilter;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by gbromfie on 9/2/16.
 */
public class PumlOutputContext implements OutputContext {
    final static public SimpleDateFormat DateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS");

    private PumlProperties _props;
    private File _pumlOutputFile;
    private PrintWriter _pumlOutputWriter;
    private File _pngOutputFile;
    private OutputRecordSet _logItems;
    private HashMap<String, String> _mapper;
    private HashMap<String, Object> _statsMap;
    private String _buffer;

    public PumlOutputContext(PumlProperties props) {
        _props = props;
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

    @Override
    public void init() throws Exception {
        if (_props.getPumlFilename() != null) {
            _pumlOutputFile = new File(_props.getPumlFilename());
            if (_pumlOutputFile.exists()) {
                _pumlOutputFile.delete();
            }
            _pumlOutputWriter = new PrintWriter(new BufferedWriter(new FileWriter(_pumlOutputFile)));
        }
        if (_props.getPngFilename() != null) {
            _pngOutputFile = new File(_props.getPngFilename());
            if (_pngOutputFile.exists()) {
                _pngOutputFile.delete();
            }
        }

        Date currentDate = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String hostname = LogX.getHostname();
        if (hostname == null) {
            getOutputRecordSet().addFirst("== Generated on %s ==\n", fmt.format(currentDate));
        } else {
            getOutputRecordSet().addFirst("== Generated on %s on host %s ==\n", fmt.format(currentDate), hostname);
        }
        for(int i = 0; i < _props.getInputFiles().size(); i++) {
            getOutputRecordSet().addFirst("== Input File %s ==\n", _props.getInputFiles().get(i).getAbsolutePath());
        }

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
    public void close() throws Exception {
        if (_mapper != null) {
            for(String key : _mapper.keySet()) {
                String value = _mapper.get(key);
                // remember to remove underscores
                _logItems.addLast("== Session Id %s maps to TID %s ==\n", key.substring(1, key.length() - 1), value);
            }
        }
        _logItems.addLast("@enduml\n");

        if (_props.getPumlFilename() != null) {
            _pumlOutputWriter.print(logItemsToString());
            _pumlOutputWriter.flush();
            _pumlOutputWriter.close();
        }

        if (_props.getPngFilename() != null) {
            SourceStringReader reader = new SourceStringReader(toString());
            String desc = reader.generateImage(_pngOutputFile);
            if (desc != null) {
                System.out.println("PlantUML: " + desc);
            }
        }
    }

    @Override
    public void onFinally() {
        if ((_props.getPumlFilename() != null) && (_pumlOutputWriter != null)) {
            _pumlOutputWriter.close();
        }
    }

    @Override
    public int size() { return _logItems.size(); }

    @Override
    public  String logItemsToString() {
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
            _buffer = bldr.toString();
        }
        return _buffer;
    }

    public static void main(String[] args) {
        PumlCommandLineProcessor clp = new PumlCommandLineProcessor();
        PumlProperties props = (PumlProperties)clp.parse(args);

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

        PumlOutputContext ctx = new PumlOutputContext(props);
        LogX logx = new LogX(props);
        logx.run();
    }
}
