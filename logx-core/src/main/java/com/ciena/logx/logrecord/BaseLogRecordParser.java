package com.ciena.logx.logrecord;

import com.ciena.logx.output.OutputContext;
import com.grb.parseutils.CharacterList;
import com.grb.parseutils.ParseContext;
import com.grb.parseutils.TextParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by gbromfie on 5/11/16.
 */
abstract public class BaseLogRecordParser implements LogRecordParser {

    static public boolean debug = false;

    final static protected SimpleDateFormat DateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    protected static final TextParser optionalSpacesParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar(' ')
            .setDelimiterChars(CharacterList.ALL_CHARS)
            .removeDelimeterChar(' ')
            .includeDelimiter(false);

    protected static final TextParser mandatorySpacesParser = new TextParser(optionalSpacesParser)
            .setLengths(1, Integer.MAX_VALUE);

    protected static final TextParser dateParser = new TextParser()
            .setAllowedChars(CharacterList.NUMBER_CHARS)
            .addAllowedChar('-')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(10, 10);

    protected static final TextParser timeParser = new TextParser()
            .setAllowedChars(CharacterList.NUMBER_CHARS)
            .addAllowedChar(':')
            .addAllowedChar(',')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(12, 12);

    protected static final TextParser textFieldParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false);

    protected static final TextParser bracketParser = new TextParser()
            .setDelimiterStrings("[", "]")
            .setAllowedChars(CharacterList.ALPHABETIC_CHARS)
            .includeDelimiter(true);

    protected static final TextParser LessGreaterThanParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar('<')
            .addAllowedChar('>')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(1, 1);

    protected OutputContext _outputContext;

    protected BaseLogRecordParser(OutputContext outputContext) {
        _outputContext = outputContext;
    }

    public void writeLegend() {}

    protected String parse(TextParser parser, ParseContext pc) throws ParseException {
        String result = parser.parse(pc);
        if (debug) {
            System.out.println(result);
        }
        return result;
    }

    protected String getTid(String tidFromMsg, String ncid) {
        String tid = tidFromMsg;
        if ((tid == null) || (tid.trim().length() == 0)) {
            tid = _outputContext.get(ncid);
            if (tid == null) {
                tid = ncid;
            }
        }
        return tid;
    }

    @Override
    public void conflate() {
    }
}
