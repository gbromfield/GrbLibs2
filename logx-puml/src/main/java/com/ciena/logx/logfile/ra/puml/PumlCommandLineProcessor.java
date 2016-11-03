package com.ciena.logx.logfile.ra.puml;

import com.ciena.logx.CommandLineProcessor;
import com.ciena.logx.LogXProperties;
import com.ciena.logx.logfile.ra.puml.logrecord.KafkaLogRecordParser;
import com.ciena.logx.logfile.ra.puml.logrecord.SessionStateRecordParser;

/**
 * Created by gbromfie on 11/2/16.
 */
public class PumlCommandLineProcessor extends CommandLineProcessor {
    public PumlCommandLineProcessor() {
        super();
        syntax.add("-puml pumlOutputFile (puml output file)\n");
        syntax.add("-png pngOutputFile (png output file)\n");
        syntax.add("-kafka (include kafka publishing logs)\n");
        syntax.add("-session (include session state logs)\n");
    }

    @Override
    protected LogXProperties createProperties() {
        return new PumlPropertiesImpl();
    }

    @Override
    protected int parseArgument(LogXProperties props, String[] args, int index) {
        if (args[index].equalsIgnoreCase("-puml")) {
            processingInputFiles = false;
            ((PumlProperties) props).setPumlFilename(args[index + 1]);
            return index + 2;
        } else if (args[index].equalsIgnoreCase("-png")) {
            processingInputFiles = false;
            ((PumlProperties) props).setPngFilename(args[index + 1]);
            return index + 2;
        } else if (args[index].equalsIgnoreCase("-kafka")) {
            processingInputFiles = false;
            props.addParserName(KafkaLogRecordParser.class.getName());
            return index + 1;
        } else if (args[index].equalsIgnoreCase("-session")) {
            processingInputFiles = false;
            props.addParserName(SessionStateRecordParser.class.getName());
            return index + 1;
        } else {
            return super.parseArgument(props, args, index);
        }
    }
}
