package com.ciena.logx.logfile.ra.flirc2;

import com.ciena.logx.CommandLineProcessor;
import com.ciena.logx.LogXProperties;

/**
 * Created by gbromfie on 11/1/16.
 */
public class Flirc2LogXCommandLineProcessor extends CommandLineProcessor {

    public Flirc2LogXCommandLineProcessor() {
        super();
        syntax.add("-rec recordingFile (output recording file)\n");
    }

    @Override
    protected LogXProperties createProperties() {
        return new Flirc2LogXPropertiesImpl();
    }

    @Override
    protected int parseArgument(LogXProperties props, String[] args, int index) {
        if (args[index].equalsIgnoreCase("-rec")) {
            processingInputFiles = false;
            ((Flirc2LogXProperties)props).setRecordingFilename(args[index + 1]);
            return index + 2;
//        } else if (args[index].equalsIgnoreCase("-session")) {
//            if (props.parsers == null) {
//                props.parsers = new ArrayList<String>();
//            }
//            processingInputFiles = false;
//            props.parsers.add(SessionStateRecordParser.class.getName());
//            return index + 1;
        } else {
            return super.parseArgument(props, args, index);
        }
    }
}
