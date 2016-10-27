package com.ciena.logx.logfile.ra.insidious.logrecord;

import com.ciena.logx.logrecord.BaseLogRecordParser;
import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.grb.parseutils.ParseContext;
import com.grb.tl1.TL1AOMessage;
import com.grb.tl1.TL1AckMessage;
import com.grb.tl1.TL1AgentDecoder;
import com.grb.tl1.TL1InputMessage;
import com.grb.tl1.TL1ManagerDecoder;
import com.grb.tl1.TL1Message;
import com.grb.tl1.TL1MessageMaxSizeExceededException;
import com.grb.tl1.TL1OutputMessage;
import com.grb.tl1.TL1ResponseMessage;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Created by gbromfie on 5/9/16.
 */
public class TL1LogRecordParser extends BaseLogRecordParser {

    private TL1AgentDecoder _agentDecoder;
    private TL1ManagerDecoder _mgrDecoder;

    public TL1LogRecordParser(OutputContext outputContext) {
        super(outputContext);
        _agentDecoder = new TL1AgentDecoder();
        _mgrDecoder = new TL1ManagerDecoder();
        TL1OutputMessage.MAX_SIZE = 1000000;
    }

    public void writeLegend() {
    }

    @Override
    public boolean parse(LogRecord logRecord) {
        ParseContext pc = new ParseContext(logRecord.getBuffer(), 0, logRecord.getLength());
        try {
            String dateStr = parse(dateParser, pc);
            parse(mandatorySpacesParser, pc);
            String timeStr = parse(timeParser, pc);
            String fullDateTime = dateStr + " " + timeStr;
            Date date = DateFormatter.parse(fullDateTime);
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
            ncid = "_" + ncid + "_";  // Add _<ncid>_
            parse(mandatorySpacesParser, pc);
            String subcomponent = null;
            try {
                subcomponent = parse(bracketParser, pc);       // Sub-Component
            } catch(ParseException e) {
                String field1 = parse(textFieldParser, pc);
                parse(mandatorySpacesParser, pc);
                String field2 = parse(textFieldParser, pc);
                parse(mandatorySpacesParser, pc);
                String field3 = parse(textFieldParser, pc);
                if (field1.equalsIgnoreCase("Ack:")) {
                    _outputContext.getOutputRecordSet().add(date, "{\n" +
                            "  \"protocol\": \"tl1\",\n" +
                            "  \"timestamp\": \"%s\",\n" +
                            "  \"input\": \"%s %s\\r\\n<\"\n" +
                            "},", fullDateTime, field2, field3);
                    return true;
                } else if (field3.equalsIgnoreCase("act-user")) {
                    String tid = getTid(null, ncid);
                    _outputContext.getOutputRecordSet().add(date, "{\n  \"protocol\": \"tl1\",\n  \"timestamp\": \"%s\",\n  \"input\": \"ACT-USER:\\\"%s\\\":ADMIN:10001::ADMIN;\"\n},\n", fullDateTime, tid);
                    _outputContext.getOutputRecordSet().add(date, "{\n  \"protocol\": \"tl1\",\n  \"timestamp\": \"%s\",\n  \"output\": \"\\r\\n\\n   \\\"%s\\\" 16-10-19 14:19:37\\r\\nM  10001 COMPLD\\r\\n;\"\n},\n", fullDateTime, tid);
                    return true;
                } else {
                    throw e;
                }
            }
            if (!subcomponent.equalsIgnoreCase("[TL1Endpoint]")) {
                return false;
            }
            parse(mandatorySpacesParser, pc);
            String lessGreaterThan = null;
            try {
                lessGreaterThan = parse(LessGreaterThanParser, pc);
            } catch(ParseException e) {
                String tmp = parse(textFieldParser, pc);
                tmp = tmp + parse(mandatorySpacesParser, pc);
                tmp = tmp + parse(textFieldParser, pc);
                tmp = tmp + parse(mandatorySpacesParser, pc);
                tmp = tmp + parse(textFieldParser, pc);
                if (tmp.equalsIgnoreCase("autonomous message received:")) {
                    lessGreaterThan = "<";
                } else {
                    throw e;
                }
            }

            if (lessGreaterThan.equals(">")) {
                parse(mandatorySpacesParser, pc);
            }

            OutputRecord or = new OutputRecord(date, this.getClass(), null, null);
            or.putEnvValue("ncid", ncid);
            or.putEnvValue("timeStr", timeStr);

            // run the rest through a TL1 Parser
            ByteBuffer tl1Buffer = ByteBuffer.allocate(pc.length);
            tl1Buffer.put(pc.buffer, pc.mark, pc.length - pc.mark);
            tl1Buffer.flip();
            try {
                if (lessGreaterThan.equals("<")) {
                    TL1Message tl1Msg = _agentDecoder.decodeTL1Message(tl1Buffer);
                    if (tl1Msg instanceof TL1AckMessage) {
                        TL1AckMessage tl1AckMsg = (TL1AckMessage)tl1Msg;
                        or.setLogString(String.format("{\n  \"protocol\": \"tl1\",\n  \"timestamp\": \"%s\",\n  \"output\": \"%s\"\n},\n", fullDateTime, transliterateCRLF(tl1AckMsg.toString())));
                        _outputContext.getOutputRecordSet().add(or);
                    } else if (tl1Msg instanceof TL1AOMessage) {
                        TL1AOMessage tl1AOMsg = (TL1AOMessage)tl1Msg;
                        _outputContext.put(ncid, tl1AOMsg.getTid());
                        or.setLogString(String.format("{\n  \"protocol\": \"tl1\",\n  \"timestamp\": \"%s\",\n  \"output\": \"%s\"\n},\n", fullDateTime, transliterateCRLF(tl1AOMsg.toString())));
                        _outputContext.getOutputRecordSet().add(or);
                    } else if (tl1Msg instanceof TL1ResponseMessage) {
                        TL1ResponseMessage tl1RespMsg = (TL1ResponseMessage)tl1Msg;
                        _outputContext.put(ncid, tl1RespMsg.getTid());
                        or.setLogString(String.format("{\n  \"protocol\": \"tl1\",\n  \"timestamp\": \"%s\",\n  \"output\": \"%s\"\n},\n", fullDateTime, transliterateCRLF(tl1RespMsg.toString())));
                        _outputContext.getOutputRecordSet().add(or);
                    }
                } else if (lessGreaterThan.equals(">")) {
                    TL1InputMessage tl1Msg = (TL1InputMessage)_mgrDecoder.decodeTL1Message(tl1Buffer);
                    String tid = getTid(tl1Msg.getTid(), ncid);
                    or.setLogString(String.format("{\n  \"protocol\": \"tl1\",\n  \"timestamp\": \"%s\",\n  \"input\": \"%s\"\n},\n", fullDateTime, transliterateCRLF(tl1Msg.toString())));
                    _outputContext.getOutputRecordSet().add(or);
                }
            } catch(ParseException e) {
                // TL1 didn't parse - report error
                e.printStackTrace();
            } catch(TL1MessageMaxSizeExceededException e) {
                // TL1 maximum message size was exceeded - report error
                e.printStackTrace();
            }
            return true;
        } catch (ParseException e) {
            // Log record doesn't match! Just discard.
        }
        return false;
    }

    private String transliterateCRLF(String input) {
        char[] inputChars = input.toCharArray();
        StringBuilder bldr = new StringBuilder();
        for(int i = 0; i < inputChars.length; i++) {
            if (inputChars[i] == '\r') {
                bldr.append("\\r");
            } else if (inputChars[i] == '\n') {
                if (i > 0) {
                    bldr.append("\\n");
                }
            } else if (inputChars[i] == '"') {
                bldr.append("\\\"");
            } else if (inputChars[i] == '\\') {
                bldr.append("\\\\");
            } else {
                bldr.append(inputChars[i]);
            }
        }
        return bldr.toString();
    }
}
