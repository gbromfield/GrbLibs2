package com.ciena.logx;

import com.ciena.logx.util.ExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by gbromfie on 11/1/16.
 */
public class CommandLineProcessor {
    final Logger logger = LoggerFactory.getLogger(CommandLineProcessor.class);
    final static public SimpleDateFormat DateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS");

    protected boolean processingInputFiles;
    protected ArrayList<String> syntax;

    public CommandLineProcessor() {
        syntax = new ArrayList<String>();
        syntax.add("-? (print help)\n");
        syntax.add("-help (print help)\n");
        syntax.add("-f filelist (input log files or directory)\n");
        syntax.add("-e extensionFilter (input log file extension filter)\n");
        syntax.add("-range fromTime[,toTime] (log file time range)\n");
        syntax.add("+tids inclTidList (include tids)\n");
        syntax.add("-tids exclTidList (exclude tids)\n");
        syntax.add("+sids inclSidList (include session ids)\n");
        syntax.add("-sids exclSidList (exclude session ids)\n");
    }

    public String getSyntax() {
        StringBuilder bldr = new StringBuilder();
        for(String line : syntax) {
            bldr.append(line);
        }
        return bldr.toString();
    }

    protected LogXProperties createProperties() {
        return new LogXPropertiesImpl();
    }

    public LogXProperties parse(String[] args) {
        LogXProperties props = createProperties();
        int i = 0;
        processingInputFiles = false;
        while(i < args.length) {
            int next = parseArgument(props, args, i);
            if(next == i) {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("Unknown command line argument \"%s\"", args[i]));
                }
                props.setUnknownArg(args[i]);
                return props;
            } else {
                i = next;
            }
        }
        if (props.getInclusive() == null) {
            props.setInclusive(false);
        }
        return props;
    }

    protected int parseArgument(LogXProperties props, String[] args, int index) {
        if (args[index].equalsIgnoreCase("-?")) {
            processingInputFiles = true;
            props.setPrintHelp(true);
            return index + 1;
        } else if (args[index].equalsIgnoreCase("-help")) {
            processingInputFiles = true;
            props.setPrintHelp(true);
            return index + 1;
        } else if (args[index].equalsIgnoreCase("-f")) {
            processingInputFiles = true;
            return index + 1;
        } else if (args[index].equalsIgnoreCase("-e")) {
            if (index == args.length - 1) {
                throw new IllegalArgumentException("Command line overflow [-e] requires a parameter]");
            }
            processingInputFiles = false;
            props.setFilter(new ExtensionFilter(args[index + 1]));
            return index + 2;
        } else if (args[index].equalsIgnoreCase("-range")) {
            if (index == args.length - 1) {
                throw new IllegalArgumentException("Command line overflow [-range] requires a parameter]");
            }
            processingInputFiles = false;
            props.setDateRange(parseDateRange(args[index + 1]));
            return index + 2;
        } else if (args[index].equalsIgnoreCase("+tids")) {
            if (index == args.length - 1) {
                throw new IllegalArgumentException("Command line overflow [+tids] requires a parameter]");
            }
            processingInputFiles = false;
            if ((props.getInclusive() != null) && (!props.getInclusive().booleanValue())) {
                throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [+tids]");
            }
            props.setIncTids(parseStringToSet(args[index + 1]));
            props.setInclusive(true);
            return index + 2;
        } else if (args[index].equalsIgnoreCase("-tids")) {
            if (index == args.length - 1) {
                throw new IllegalArgumentException("Command line overflow [-tids] requires a parameter]");
            }
            processingInputFiles = false;
            if ((props.getInclusive() != null) && (props.getInclusive().booleanValue())) {
                throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [-tids]");
            }
            props.setExclTids(parseStringToSet(args[index + 1]));
            props.setInclusive(false);
            return index + 2;
        } else if (args[index].equalsIgnoreCase("+sids")) {
            if (index == args.length - 1) {
                throw new IllegalArgumentException("Command line overflow [+sids] requires a parameter]");
            }
            processingInputFiles = false;
            if ((props.getInclusive() != null) && (!props.getInclusive().booleanValue())) {
                throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [+sids]");
            }
            props.setIncSids(parseStringToSet(args[index + 1]));
            props.setInclusive(true);
            return index + 2;
        } else if (args[index].equalsIgnoreCase("-sids")) {
            if (index == args.length - 1) {
                throw new IllegalArgumentException("Command line overflow [-sids] requires a parameter]");
            }
            processingInputFiles = false;
            if ((props.getInclusive() != null) && (props.getInclusive().booleanValue())) {
                throw new IllegalArgumentException("Cannot include inclusive and exclusive parameters [-sids]");
            }
            props.setExclSids(parseStringToSet(args[index + 1]));
            props.setInclusive(false);
            return index + 2;
        } else if (processingInputFiles) {
            props.addInputFilename(args[index]);
            return index + 1;
        }
        return index;
    }

    public static HashSet<String> parseStringToSet(String arg) {
        HashSet<String> set = new HashSet<String>();
        String[] argArr = arg.split(",");
        for(int j = 0; j < argArr.length; j++) {
            set.add(argArr[j]);
        }
        return set;
    }

    public static Date[] parseDateRange(String arg) {
        Date[] dateRange = new Date[2];
        dateRange[0] = null;
        dateRange[1] = null;
        String[] argArr = arg.split(",");
        if ((argArr.length != 1) && (argArr.length != 2)) {
            throw new IllegalArgumentException("Range argument requires one or two parameters \"fromTime[,toTime]\"");
        }
        String fromDateStr = argArr[0].trim();
        if (!fromDateStr.isEmpty()) {
            try {
                dateRange[0] = DateFormatter.parse(fromDateStr);
            } catch (ParseException e) {
                throw new IllegalArgumentException(String.format("Could not parse from date \"%s\" from format \"%s\"", fromDateStr, DateFormatter.toPattern()), e);
            }
        }
        if (argArr.length == 2) {
            String toDateStr = argArr[1].trim();
            if (!toDateStr.isEmpty()) {
                try {
                    dateRange[1] = DateFormatter.parse(toDateStr);
                } catch (ParseException e) {
                    throw new IllegalArgumentException(String.format("Could not parse to date \"%s\" from format \"%s\"", toDateStr, DateFormatter.toPattern()), e);
                }
            }
        }
        return dateRange;
    }
}
