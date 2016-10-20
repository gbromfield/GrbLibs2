package com.ciena.logx.logfile.ra.puml.logrecord;

import com.ciena.logx.logrecord.BaseLogRecordParser;
import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.ciena.logx.output.OutputRecordSet;
import com.grb.parseutils.ParseContext;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Created by gbromfie on 5/9/16.
 */
public class KafkaLogRecordParser extends BaseLogRecordParser {

    public KafkaLogRecordParser(OutputContext outputContext) {
        super(outputContext);
    }

    public void writeLegend() {
        _outputContext.getOutputRecordSet().addFirst("note over KAFKA\nKafka Syntax: [time,tid,topic,# msgs,duration]\nend note\n");
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
                        or.setLogType("startEqptSync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Equipment Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogType("endEqptSync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Equipment Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    }
                } else if (msg.contains("\"object_type\": \"tpes\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogType("startTPESync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s TPE Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogType("endTPESync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s TPE Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    }
                } else if (msg.contains("\"object_type\": \"fres\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogType("startFRESync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s FRE Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogType("endFRESync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s FRE Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    }
                }
            } else if (topic.equalsIgnoreCase("bp.ra.v1.alarms")) {
                String msg = new String(pc.buffer, 0, pc.length);
                if (msg.contains("\"object_type\": \"alarm\"")) {
                    if (msg.contains("\"op\": \"start\"")) {
                        or.setLogType("startAlarmSync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Alarm Sync Started\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    } else if (msg.contains("\"op\": \"complete\"")) {
                        or.setLogType("endAlarmSync");
                        or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s Alarm Sync Completed\n", ncid, timeStr));
                        _outputContext.getOutputRecordSet().add(or);
                        return true;
                    }
                }
            }

            or.setLogType("other");
            or.setLogString(String.format("\"%s\" -> \"KAFKA\" : %s %s\n", ncid, timeStr, topic));
            _outputContext.getOutputRecordSet().add(or);

            return true;
        } catch (ParseException e) {
            // Log record doesn't match! Just discard.
        }
        return false;
    }

    @Override
    public void conflate() {
        OutputRecordSet set = _outputContext.getOutputRecordSet();
//        System.out.println("############# Start Full Set");
//        for(int i = 0; i < set.size(); i++) {
//            System.out.print(String.format("%d -- %s", i, set.get(i).getLogString()));
//        }
//        System.out.println("############# End Full Set");
        int startIndex = -1;
        int i = 0;
        while(i < set.size()) {
            OutputRecord rec = set.get(i);
            if (this.getClass().equals(rec.getLogClass())) {
                String logType = rec.getLogType();
                String ncid = (String)rec.getEnvValue("ncid");
                String timeStr = (String)rec.getEnvValue("timeStr");
                String topic = (String)rec.getEnvValue("topic");
                if (rec.getLogString().contains("Equipment Sync")) {
                    System.out.println();
                }
                if (startIndex == -1) {
                    // start of conflation
                    startIndex = i;
                    i++;
                } else {
                    OutputRecord start = set.get(startIndex);
                    if (!((rec.getEnvValue("ncid").equals(start.getEnvValue("ncid"))) &&
                            (rec.getEnvValue("topic").equals(start.getEnvValue("topic"))) &&
                            (logType.equals(start.getLogType())))) {
                        if (updateRecordSet(set, startIndex, i-1)) {
                            i = startIndex + 1;
                            startIndex = -1;
                        } else {
                            startIndex = i;
                            i++;
                        }
                    } else {
                        i++;
                    }
                }
            } else {
                if (startIndex != -1) {
                    if (updateRecordSet(set, startIndex, i-1)) {
                        i = startIndex + 1;
                        startIndex = -1;
                    } else {
                        startIndex = i;
                        i++;
                    }
                } else {
                    i++;
                }
            }
        }
//        System.out.println("############# Start Conflated Set");
//        for(i = 0; i < set.size(); i++) {
//            System.out.print(String.format("%d -- %s", i, set.get(i).getLogString()));
//        }
//        System.out.println("############# End Conflated Set");
    }

    public boolean updateRecordSet(OutputRecordSet set, int startIndex, int endIndex) {
        // end of conflation - only conflate if there is more than 4
        if ((endIndex - startIndex) > 4) {
            OutputRecord startRec = set.get(startIndex);
            OutputRecord endRec = set.get(endIndex);
            set.remove(startIndex, endIndex);
            set.add(startIndex, new OutputRecord(startRec.getLogDate(), this.getClass(), null, "\"%s\" -> \"KAFKA\" : %s to %s, %s msgs = %d\n",
                    startRec.getEnvValue("ncid"), startRec.getEnvValue("timeStr"), endRec.getEnvValue("timeStr"),
                    startRec.getEnvValue("topic"), endIndex - startIndex + 1));
            return true;
        }
        return false;
    }
}
