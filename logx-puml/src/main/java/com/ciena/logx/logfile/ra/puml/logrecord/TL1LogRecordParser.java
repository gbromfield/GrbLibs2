package com.ciena.logx.logfile.ra.puml.logrecord;

import com.ciena.logx.logrecord.BaseLogRecordParser;
import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.output.OutputRecord;
import com.ciena.logx.util.Time;
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
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by gbromfie on 5/9/16.
 */
public class TL1LogRecordParser extends BaseLogRecordParser {

    static private HashMap<String, Long> responseElapsedTracker = new HashMap<String, Long>();
    static private HashMap<String, Long> cumulativeElapsedTracker = new HashMap<String, Long>();
    static private HashMap<String, Long> interactionElapsedTracker = new HashMap<String, Long>();

    private TL1AgentDecoder _agentDecoder;
    private TL1ManagerDecoder _mgrDecoder;
    private boolean _tidMappingOnly;

    public TL1LogRecordParser(OutputContext outputContext, boolean tidMappingOnly) {
        super(outputContext);
        _tidMappingOnly = tidMappingOnly;
        _agentDecoder = new TL1AgentDecoder();
        _mgrDecoder = new TL1ManagerDecoder();
        TL1OutputMessage.MAX_SIZE = 1000000;
    }

    public void writeLegend() {
        if (!_tidMappingOnly) {
            _outputContext.getOutputRecordSet().addFirst("note over RA\nTL1 Request Syntax: [time,ctag,command]\n TL1Response Syntax: [time,ctag,compl code,\n    cmd elapsed, cmd cumulative,total elapsed on tid]\nend note\n");
        }
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
                    if (!_tidMappingOnly) {
                        String tid = getTid(null, ncid);
                        _outputContext.getOutputRecordSet().add(date, "\"%s\" -> %s : %s %s %s\n", tid, "RA", timeStr, field3, field2);
                    }
                    return true;
                } else if (field3.equalsIgnoreCase("act-user")) {
                    if (!_tidMappingOnly) {
                        String tid = getTid(null, ncid);
                        _outputContext.getOutputRecordSet().add(date, "\"%s\" -> %s : %s %s\n", "RA", tid, timeStr, "ACT-USER");
                    }
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
                        if (!_tidMappingOnly) {
                            TL1AckMessage tl1AckMsg = (TL1AckMessage)tl1Msg;
                            String tid = getTid(null, ncid);
                            or.setLogString(String.format("\"%s\" -> %s : %s %s %s\n", tid, "RA", timeStr, tl1AckMsg.getCTAG(), tl1AckMsg.getAckCode()));
                            _outputContext.getOutputRecordSet().add(or);
                        }
                    } else if (tl1Msg instanceof TL1AOMessage) {
                        TL1AOMessage tl1AOMsg = (TL1AOMessage)tl1Msg;
                        String tid = getTid(tl1AOMsg.getTid(), ncid);
                        String command = "?";
                        if (tl1AOMsg.getVerb() != null) {
                            command = tl1AOMsg.getVerb();
                        }
                        if ((tl1AOMsg.getMod1() != null) && (tl1AOMsg.getMod1().trim().length() > 0)) {
                            command = command + "-" + tl1AOMsg.getMod1();
                        }
                        if ((tl1AOMsg.getMod2() != null) && (tl1AOMsg.getMod2().trim().length() > 0)) {
                            command = command + "-" + tl1AOMsg.getMod2();
                        }
                        _outputContext.put(ncid, tl1AOMsg.getTid());
                        if (!_tidMappingOnly) {
                            or.setLogString(String.format("\"%s\" -> %s : %s %s %s\n", tid, "RA", timeStr, tl1AOMsg.getATAG(), command));
                            _outputContext.getOutputRecordSet().add(or);
                        }
                    } else if (tl1Msg instanceof TL1ResponseMessage) {
                        TL1ResponseMessage tl1RespMsg = (TL1ResponseMessage)tl1Msg;
                        Long reqTime = responseElapsedTracker.get(ncid);
                        String elapsedTime = "?";
                        String cummulativeTime = "?";
                        if (reqTime != null) {
                            long elapsedTimeInMS = date.getTime() - reqTime;
                            elapsedTime = Time.msToString(elapsedTimeInMS);
                            responseElapsedTracker.remove(ncid);
                            Long cummulativeTimeInMS = cumulativeElapsedTracker.get(ncid);
                            if (cummulativeTimeInMS == null) {
                                cumulativeElapsedTracker.put(ncid, elapsedTimeInMS);
                                cummulativeTime = Time.msToString(elapsedTimeInMS);
                            } else {
                                cumulativeElapsedTracker.put(ncid, elapsedTimeInMS + cummulativeTimeInMS.longValue());
                                cummulativeTime = Time.msToString(elapsedTimeInMS + cummulativeTimeInMS.longValue());
                            }
                        }
                        Long interTime = interactionElapsedTracker.get(ncid);
                        String totalElapsed = "?";
                        if (interTime != null) {
                            totalElapsed = Time.msToString(date.getTime() - interTime);
                        }
                        _outputContext.put(ncid, tl1RespMsg.getTid());
                        if (!_tidMappingOnly) {
                            or.setLogString(String.format("\"%s\" -> %s : %s %s %s (%s,%s,%s)\n", tl1RespMsg.getTid(), "RA", timeStr, tl1RespMsg.getCTAG(), tl1RespMsg.getComplCode(), elapsedTime, cummulativeTime, totalElapsed));
                            _outputContext.getOutputRecordSet().add(or);
                        }
                    }
                } else if (lessGreaterThan.equals(">")) {
                    TL1InputMessage tl1Msg = (TL1InputMessage)_mgrDecoder.decodeTL1Message(tl1Buffer);
                    String tid = getTid(tl1Msg.getTid(), ncid);
                    String command = "?";
                    if (tl1Msg.getVerb() != null) {
                        command = tl1Msg.getVerb();
                    }
                    if ((tl1Msg.getMod1() != null) && (tl1Msg.getMod1().trim().length() > 0)) {
                        command = command + "-" + tl1Msg.getMod1();
                    }
                    if ((tl1Msg.getMod2() != null) && (tl1Msg.getMod2().trim().length() > 0)) {
                        command = command + "-" + tl1Msg.getMod2();
                    }
                    // Store the time of the command
                    responseElapsedTracker.put(ncid, date.getTime());
                    if (interactionElapsedTracker.get(ncid) == null) {
                        interactionElapsedTracker.put(ncid, date.getTime());
                    }
                    if (!_tidMappingOnly) {
                        or.setLogString(String.format("%s -> \"%s\" : %s %s %s\n", "RA", tid, timeStr, tl1Msg.getCTAG(), command));
                        _outputContext.getOutputRecordSet().add(or);
                    }
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
}
