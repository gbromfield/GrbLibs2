package com.ciena.logx.logfile.ra.puml.logrecord;

import com.ciena.logx.logrecord.BaseLogRecordParser;
import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.grb.parseutils.ParseContext;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Created by gbromfie on 7/20/16.
 */
public class SessionStateRecordParser extends BaseLogRecordParser {

    public SessionStateRecordParser(OutputContext outputContext) {
        super(outputContext);
    }

    @Override
    public boolean parse(LogRecord logRecord) {
        ParseContext pc = new ParseContext(logRecord.getBuffer(), 0, logRecord.getLength());
        try {
            String dateStr = parse(dateParser, pc);
            parse(mandatorySpacesParser, pc);
            String timeStr = parse(timeParser, pc);
            Date date = DateFormatter.parse(dateStr + " " + timeStr);
            parse(mandatorySpacesParser, pc);
            parse(textFieldParser, pc);     // Log Level
            parse(mandatorySpacesParser, pc);
            String component = parse(textFieldParser, pc);      // Component
            if (component.equalsIgnoreCase("rasdk.api.sessions")) {
                return parseSDKState(pc, date, timeStr);
            } else if (component.toLowerCase(Locale.getDefault()).startsWith("bpprov")) {
                return parseExtendedState(pc, date, timeStr);
            }
        } catch (ParseException e) {
            // Log record doesn't match! Just discard.
        }
        return false;
    }

    public boolean parseSDKState(ParseContext pc, Date date, String timeStr) throws ParseException {
        parse(mandatorySpacesParser, pc);
        String ncid = parse(bracketParser, pc);
        ncid = ncid.substring(1, ncid.length()-1);  // Remove []
        ncid = "_" + ncid + "_";
        parse(mandatorySpacesParser, pc);
        String field1 = parse(textFieldParser, pc);
        if (!field1.equalsIgnoreCase("state")) {
            return false;
        }
        parse(mandatorySpacesParser, pc);
        String fromState = parse(textFieldParser, pc);
        parse(mandatorySpacesParser, pc);
        String arrow = parse(textFieldParser, pc);
        parse(mandatorySpacesParser, pc);
        String toState = parse(textFieldParser, pc);
        OutputRecord or = new OutputRecord(date, this.getClass(), null, null);
        or.putEnvValue("ncid", ncid);
        or.setLogString(String.format("\"%s\" -> \"%s\" : %s SDK %s -> %s\n", ncid, ncid, timeStr,
                fromState, toState));
        _outputContext.getOutputRecordSet().add(or);
        return true;
    }

    public boolean parseExtendedState(ParseContext pc, Date date, String timeStr) throws ParseException {
        parse(mandatorySpacesParser, pc);
        String ncid = parse(bracketParser, pc);
        ncid = ncid.substring(1, ncid.length()-1);  // Remove []
        ncid = "_" + ncid + "_";
        parse(mandatorySpacesParser, pc);
        String stateType = parse(textFieldParser, pc);
        String stateTypeStr;
        if (stateType.equalsIgnoreCase("PhysicalConnection")) {
            stateTypeStr = "Physical";
        } else if (stateType.equalsIgnoreCase("LogicalConnection")) {
            stateTypeStr = "Logical";
        } else if (stateType.equalsIgnoreCase("SshManagedTransport")) {
            stateTypeStr = "Session";
        } else {
            return false;
        }
        parse(mandatorySpacesParser, pc);
        String stateStr = parse(textFieldParser, pc);
        if (!stateStr.equalsIgnoreCase("state")) {
            return false;
        }
        parse(mandatorySpacesParser, pc);
        String changeStr = parse(textFieldParser, pc);
        if (!changeStr.equalsIgnoreCase("change:")) {
            return false;
        }
        parse(mandatorySpacesParser, pc);
        String fromState = parse(textFieldParser, pc);
        parse(mandatorySpacesParser, pc);
        String arrowStr = parse(textFieldParser, pc);
        parse(mandatorySpacesParser, pc);
        String toState = parse(textFieldParser, pc);
        OutputRecord or = new OutputRecord(date, this.getClass(), null, null);
        or.putEnvValue("ncid", ncid);
        or.setLogString(String.format("\"%s\" -> \"%s\" : %s %s %s -> %s\n", ncid, ncid, timeStr,
                stateTypeStr, fromState, toState));
        _outputContext.getOutputRecordSet().add(or);
        return true;
    }
}
