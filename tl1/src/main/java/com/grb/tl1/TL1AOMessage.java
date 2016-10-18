package com.grb.tl1;

import java.nio.ByteBuffer;
import java.text.ParseException;

import com.grb.parseutils.CharacterList;
import com.grb.parseutils.ParseContext;
import com.grb.parseutils.TextParser;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1AOMessage extends TL1OutputMessage {

    protected static final TextParser atagParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(1, Integer.MAX_VALUE);

    protected static final TextParser verbParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(1, Integer.MAX_VALUE);

    protected static final TextParser modParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false);

    private String _tid;
    private String _date;
    private String _time;
    private String _alarmCode;
    private String _atag;
    private int _atagIndex;
    private int _atagLength;
    private String _verb;
    private String _mod1;
    private String _mod2;

    public TL1AOMessage(byte[] preamble, int offset, int messageStartIdx, int length) throws TL1MessageMaxSizeExceededException {
        super(preamble, offset, messageStartIdx, length);
        _tid = null;
        _date = null;
        _time = null;
        _alarmCode = null;
        _atag = null;
        _atagIndex = 0;
        _atagLength = 0;
        _verb = null;
        _mod1 = null;
        _mod2 = null;
    }

    public String getTid() { return _tid; }

    public String getDate() { return _date; }

    public String getTime() { return _time; }

    public String getAlmCode() {
        return _alarmCode;
    }

    public String getATAG() {
        return _atag;
    }

    public void setATAG(String newATAG) {
    	int lengthDifference = newATAG.length() - _atagLength;
    	com.grb.bufferutils.ByteBuffer newBuffer = new com.grb.bufferutils.ByteBuffer(_buffer.getLength() + lengthDifference);
    	byte[] ba = _buffer.getBackingArray();
    	int length = _buffer.getLength();
    	newBuffer.write(ba, 0, _atagIndex);
    	newBuffer.write(newATAG.getBytes());
    	newBuffer.write(ba, _atagIndex + _atagLength, length - (_atagIndex + _atagLength));
    	_atag = newATAG;
    	_buffer = newBuffer;
    }

    public String getVerb() {
        return _verb;
    }

    public String getMod1() {
        return _mod1;
    }

    public String getMod2() {
        return _mod2;
    }

    @Override
    public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() > MAX_SIZE) {
                throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of output message reached", TL1OutputMessage.MAX_SIZE));
            }
            if ((b == ';') && (_stack.size() == 0)) {
                parseTL1();
                return true;
            }
        }
        return false;
    }

    private void parseTL1() throws ParseException {
        ParseContext pc = new ParseContext(_buffer.getBackingArray(), _messageStartIdx, _buffer.getLength() - _messageStartIdx);
        optionalWhitespaceParser.parse(pc);
        try {
            _tid = quotedsidParser.parse(pc);
        } catch(ParseException e) {
            _tid = sidParser.parse(pc);
        }
        manadatorySpacesParser.parse(pc);
        _date = dateParser.parse(pc);
        manadatorySpacesParser.parse(pc);
        _time = timeParser.parse(pc);
        manadatoryWhitespaceParser.parse(pc);
        _alarmCode = responseCodeParser.parse(pc, 2);
        manadatorySpacesParser.parse(pc);
        _atagIndex = pc.mark;
        _atag = atagParser.parse(pc);
        _atagLength = pc.mark - _atagIndex;
        manadatorySpacesParser.parse(pc);
        _verb = verbParser.parse(pc);
        optionalSpacesParser.parse(pc);
        _mod1 = modParser.parse(pc);
        optionalSpacesParser.parse(pc);
        _mod2 = modParser.parse(pc);
    }
}
