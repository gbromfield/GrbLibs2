package com.ciena.logx.logfile.ra.puml;

import com.ciena.logx.LogX;
import com.ciena.logx.logfile.ra.puml.logrecord.*;
import com.ciena.logx.logrecord.LogRecordParser;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.ciena.logx.output.OutputRecordSet;
import com.ciena.logx.util.ExtensionFilter;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by gbromfie on 9/2/16.
 */
public class PumlOutputContext implements OutputContext {
    final static public SimpleDateFormat DateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS");

    private ArrayList<File> _inputFileList;
    private String _pumlOutputFilename;
    private File _pumlOutputFile;
    private PrintWriter _pumlOutputWriter;
    private String _pngOutputFilename;
    private File _pngOutputFile;
    private LogRecordParser[] _parsers;
    private OutputRecordSet _logItems;
    private HashMap<String, String> _mapper;
    private HashMap<String, Object> _statsMap;
    private String _bufferStr;
    private HashSet<String> _incTids;
    private HashSet<String> _exclTids;
    private HashSet<String> _incSids;
    private HashSet<String> _exclSids;
    private Boolean _inclusive;

    public PumlOutputContext(String[] args, ArrayList<File> inputFileList) {
        ArrayList<LogRecordParser> parserList = new ArrayList<LogRecordParser>();
        _inputFileList = inputFileList;
        _pumlOutputFilename = null;
        _pumlOutputFile = null;
        _pumlOutputWriter = null;
        _pngOutputFilename = null;
        _pngOutputFile = null;
        _incTids = null;
        _exclTids = null;
        _incSids = null;
        _exclSids = null;
        _inclusive = null;
        Date fromDate = null;
        Date toDate = null;
        boolean tl1 = false;
        int i = 0;
        while(i < args.length) {
            if (args[i].equalsIgnoreCase("-tl1")) {
                TL1LogRecordParser parser = new TL1LogRecordParser(this, false);
                parserList.add(parser);
                tl1 = true;
            } else if (args[i].equalsIgnoreCase("-puml")) {
                i++;
                _pumlOutputFilename = args[i];
            } else if (args[i].equalsIgnoreCase("-png")) {
                i++;
                _pngOutputFilename = args[i];
            } else if (args[i].equalsIgnoreCase("-kafka")) {
                KafkaLogRecordParser parser = new KafkaLogRecordParser(this);
                parserList.add(parser);
            } else if (args[i].equalsIgnoreCase("-session")) {
                SessionStateRecordParser parser = new SessionStateRecordParser(this);
                parserList.add(parser);
            } else if (args[i].equalsIgnoreCase("-range")) {
                i++;
                String[] argArr = args[i].split(",");
                if ((argArr.length != 1) && (argArr.length != 2)) {
                    throw new IllegalArgumentException("Range argument requires one or two parameters \"fromTime[,toTime]\"");
                }
                String fromDateStr = argArr[0].trim();
                if (!fromDateStr.isEmpty()) {
                    try {
                        fromDate = DateFormatter.parse(fromDateStr);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException(String.format("Could not parse from date \"%s\" from format \"%s\"", fromDateStr, DateFormatter.toPattern()), e);
                    }
                }
                if (argArr.length == 2) {
                    String toDateStr = argArr[1].trim();
                    if (!toDateStr.isEmpty()) {
                        try {
                            toDate = DateFormatter.parse(toDateStr);
                        } catch (ParseException e) {
                            throw new IllegalArgumentException(String.format("Could not parse to date \"%s\" from format \"%s\"", toDateStr, DateFormatter.toPattern()), e);
                        }
                    }
                }
            } else if (args[i].equalsIgnoreCase("+tids")) {
                if ((_inclusive != null) && (!_inclusive)) {
                    throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [+tids]");
                }
                i++;
                String[] argArr = args[i].split(",");
                if (_incTids == null) {
                    _incTids = new HashSet<String>();
                }
                for(int j = 0; j < argArr.length; j++) {
                    _incTids.add(argArr[j]);
                }
                _inclusive = true;
            } else if (args[i].equalsIgnoreCase("-tids")) {
                if ((_inclusive != null) && (_inclusive)) {
                    throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [-tids]");
                }
                i++;
                String[] argArr = args[i].split(",");
                if (_exclTids == null) {
                    _exclTids = new HashSet<String>();
                }
                for(int j = 0; j < argArr.length; j++) {
                    _exclTids.add(argArr[j]);
                }
                _inclusive = false;
            } else if (args[i].equalsIgnoreCase("+sids")) {
                if ((_inclusive != null) && (!_inclusive)) {
                    throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [+sids]");
                }
                i++;
                String[] argArr = args[i].split(",");
                if (_incSids == null) {
                    _incSids = new HashSet<String>();
                }
                for(int j = 0; j < argArr.length; j++) {
                    _incSids.add(argArr[j]);
                }
                _inclusive = true;
            } else if (args[i].equalsIgnoreCase("-sids")) {
                if ((_inclusive != null) && (_inclusive)) {
                    throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [-sids]");
                }
                i++;
                String[] argArr = args[i].split(",");
                if (_exclSids == null) {
                    _exclSids = new HashSet<String>();
                }
                for(int j = 0; j < argArr.length; j++) {
                    _exclSids.add(argArr[j]);
                }
                _inclusive = false;
            }
            i++;
        }
        if (parserList.isEmpty()) {
            throw new IllegalArgumentException("No Puml parsers specified, need one or more of [-tl1, -kafka, -session, -sync]");
        }

        if ((_pumlOutputFilename == null) || (_pngOutputFilename == null)) {
            throw new IllegalArgumentException("Error: No output file specified, must specify -puml or -png options");
        }

        if (!tl1) {
            // add a TL1 parser just for TID mapping
            TL1LogRecordParser parser = new TL1LogRecordParser(this, true);
            parserList.add(parser);
        }
        _parsers = new LogRecordParser[parserList.size()];
        _parsers = parserList.toArray(_parsers);
        _mapper = new HashMap<String, String>();
        _statsMap = new HashMap<String, Object>();
        _bufferStr = null;
        _logItems = new OutputRecordSet(fromDate, toDate);
        _logItems.addFirst("@startuml\n");
        if (_inclusive == null) {
            _inclusive = false;
        }
    }

    @Override
    public void init() throws Exception {
        if (_pumlOutputFilename != null) {
            _pumlOutputFile = new File(_pumlOutputFilename);
            if (_pumlOutputFile.exists()) {
                _pumlOutputFile.delete();
            }
            _pumlOutputWriter = new PrintWriter(new BufferedWriter(new FileWriter(_pumlOutputFile)));
        }
        if (_pngOutputFilename != null) {
            _pngOutputFile = new File(_pngOutputFilename);
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
        for(int i = 0; i < _inputFileList.size(); i++) {
            getOutputRecordSet().addFirst("== Input File %s ==\n", _inputFileList.get(i).getAbsolutePath());
        }

    }

    public boolean passesFilter(String ncid) {
        if (ncid == null) {
            return true;
        }
        String ncidNo_ = ncid.substring(1, ncid.length()-1);
        if (_incSids != null) {
            return _incSids.contains(ncidNo_);
        }
        if (_exclSids != null) {
            return !_exclSids.contains(ncidNo_);
        }
        String tid = _mapper.get(ncid);
        if (tid == null) {
            if (_inclusive) {
                return false;
            } else {
                return true;
            }
        }
        if (_incTids != null) {
            return _incTids.contains(tid);
        }
        if (_exclTids != null) {
            return !_exclTids.contains(tid);
        }
        if (_inclusive) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public LogRecordParser[] getLogRecordParsers() {
        return _parsers;
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

        if (_pumlOutputFilename != null) {
            _pumlOutputWriter.print(toString());
            _pumlOutputWriter.flush();
            _pumlOutputWriter.close();
        }

        if (_pngOutputFilename != null) {
            SourceStringReader reader = new SourceStringReader(toString());
            String desc = reader.generateImage(_pngOutputFile);
            if (desc != null) {
                System.out.println("PlantUML: " + desc);
            }
        }
    }

    @Override
    public void onFinally() {
        if ((_pumlOutputFilename != null) && (_pumlOutputWriter != null)) {
            _pumlOutputWriter.close();
        }
    }

    @Override
    public int size() { return _logItems.size(); }

    @Override
    public String toString() {
        if (_bufferStr == null) {
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
            _bufferStr = bldr.toString();
        }
        if (_bufferStr == null) {
            return super.toString();
        }
        return _bufferStr;
    }

    public static void main(String[] args) {
        final String syntax = "Syntax: LogX -i <input file> -puml <output text file> -png <output png file> -e <log file extension>";

        ArrayList<String> inputFiles = new ArrayList<String>();
        boolean processingInputFiles = false;
        String extension = null;
        ExtensionFilter filter = null;

        int i = 0;
        while(i < args.length) {
            if (args[i].equalsIgnoreCase("-i")) {
                processingInputFiles = true;
            } else if (args[i].equalsIgnoreCase("-e")) {
                processingInputFiles = false;
                i++;
                filter = new ExtensionFilter(args[i]);
            } else if (args[i].startsWith("-")) {
                processingInputFiles = false;
            } else if (processingInputFiles) {
                inputFiles.add(args[i]);
            } else {
                // ignore might be a parser argument
            }
            i++;
        }

        if (inputFiles.size() == 0) {
            System.out.println(syntax);
            System.out.println("Error: No input file specified");
            System.exit(1);
        }

        ArrayList<File>  inputFileList = LogX.processFilenames(inputFiles, filter);
        PumlOutputContext ctx = new PumlOutputContext(args, inputFileList);
        LogX logx = new LogX(inputFileList, ctx);
        logx.run();
    }
}
