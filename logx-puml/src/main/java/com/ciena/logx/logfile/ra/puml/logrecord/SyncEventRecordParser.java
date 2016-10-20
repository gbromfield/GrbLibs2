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
 * Created by gbromfie on 9/8/16.
 */
public class SyncEventRecordParser extends BaseLogRecordParser {

    public SyncEventRecordParser(OutputContext outputContext) {
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
            if (!component.toLowerCase(Locale.getDefault()).startsWith("bpprov")) {
                return false;
            }
            parse(mandatorySpacesParser, pc);
            String ncid = parse(bracketParser, pc);
            ncid = ncid.substring(1, ncid.length()-1);  // Remove []
            ncid = "_" + ncid + "_";
            parse(mandatorySpacesParser, pc);
            String subcomponent = null;
            subcomponent = parse(bracketParser, pc);       // Sub-Component
            if (!subcomponent.equalsIgnoreCase("[KafkaTransport]")) {
                return false;
            }
            parse(mandatorySpacesParser, pc);
            String lessGreaterThan = parse(LessGreaterThanParser, pc);

            if (!lessGreaterThan.equals(">")) {
                return false;
            }

            parse(mandatorySpacesParser, pc);
            String topic = parse(textFieldParser, pc);

            OutputRecord or = new OutputRecord(date, this.getClass(), null, null);
            or.putEnvValue("ncid", ncid);
            or.putEnvValue("timeStr", timeStr);
            or.putEnvValue("topic", topic);

            if (topic.equalsIgnoreCase("bp.ra.v1.sync")) {
                String msg = new String(pc.buffer, 0, pc.length);
                if (msg.contains("\"object_type\": \"equipment\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Equipment Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Equipment Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    }
                } else if (msg.contains("\"object_type\": \"tpes\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s TPE Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s TPE Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    }
                } else if (msg.contains("\"object_type\": \"fres\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s FRE Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s FRE Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    }
                } else {
                    return false;
                }
            } else if (topic.equalsIgnoreCase("bp.ra.v1.alarms")) {
                String msg = new String(pc.buffer, 0, pc.length);
                if (msg.contains("\"object_type\": \"alarm\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Alarm Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Alarm Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        } catch (ParseException e) {
            // Log record doesn't match! Just discard.
        }
        return false;
    }
}
