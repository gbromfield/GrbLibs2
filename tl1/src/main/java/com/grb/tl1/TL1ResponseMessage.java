package com.grb.tl1;

import java.nio.ByteBuffer;
import java.text.ParseException;

import com.grb.parseutils.CharacterList;
import com.grb.parseutils.ParseContext;
import com.grb.parseutils.TextParser;

/**
 * Created by gbromfie on 11/4/15.
 */
public class TL1ResponseMessage extends TL1OutputMessage {

    private static final TextParser completonCodeParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false);

    private static final TextParser eolParser = new TextParser()
            .setAllowedChars(CharacterList.CR_LF_CHARS)
            .setDelimiterChars(CharacterList.ALL_CHARS)
            .removeDelimeterChars(CharacterList.CR_LF_CHARS)
            .includeDelimiter(false);

    private static final TextParser bodyParser = new TextParser()
            .setAllowedChars(CharacterList.ALL_CHARS)
            .removeAllowedChar(';')
            .removeAllowedChar('>')
            .addDelimeterChar(';')
            .addDelimeterChar('>')
            .includeDelimiter(false);

    private String _tid;
    private String _date;
    private String _time;
    private String _ctag;
    private int _ctagIndex;
    private int _ctagLength;
    private String _complCode;
    private String _body;
    private TL1ResponseType _responseType;

    public TL1ResponseMessage(byte[] preamble, int offset, int messageStartIdx, int length) throws TL1MessageMaxSizeExceededException {
        super(preamble, offset, messageStartIdx, length);
        _tid = null;
        _date = null;
        _time = null;
        _ctag = null;
        _ctagIndex = 0;
        _ctagLength = 0;
        _complCode = null;
        _body = null;
        _responseType = null;
    }

    public String getTid() { return _tid; }

    public String getDate() {return _date; }

    public String getTime() {return _time; }

    public String getCTAG() {
        return _ctag;
    }

    public void setCTAG(String newCTAG) {
    	int lengthDifference = newCTAG.length() - _ctagLength;
    	com.grb.bufferutils.ByteBuffer newBuffer = new com.grb.bufferutils.ByteBuffer(_buffer.getLength() + lengthDifference);
    	byte[] ba = _buffer.getBackingArray();
    	int length = _buffer.getLength();
    	newBuffer.write(ba, 0, _ctagIndex);
    	newBuffer.write(newCTAG.getBytes());
    	newBuffer.write(ba, _ctagIndex + _ctagLength, length - (_ctagIndex + _ctagLength));
    	_ctag = newCTAG;
    	_ctagLength = _ctag.length();
    	_buffer = newBuffer;
    }
    
    public String getComplCode() { return _complCode; }

    public String getBody() { return _body; }

    public TL1ResponseType getResponseType() {
        return _responseType;
    }

    @Override
    public boolean parse(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() > MAX_SIZE) {
                throw new TL1MessageMaxSizeExceededException(String.format("Error: maximum %d character size of output message reached", TL1OutputMessage.MAX_SIZE));
            }
            if ((_responseType = isTerminal(_buffer.getBackingArray(), _buffer.getLength())) != null) {
                parseTL1();
                return true;
            }
        }
        return false;
    }

    private TL1ResponseType isTerminal(byte[] ba, int length) {
        if (length >= 3) {
            if ((ba[length-3] == '\r') && (ba[length-2] == '\n')) {
                if (ba[length-1] == ';') {
                    return TL1ResponseType.TERMINATION;
                } else if (ba[length-1] == '>') {
                    return TL1ResponseType.CONTINUATION;
                }
            }
        }
        return null;
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
        responseCodeParser.parse(pc, 2);
        manadatorySpacesParser.parse(pc);
        _ctagIndex = pc.mark;
        _ctag = ctagParser.parse(pc);
        _ctagLength = pc.mark - _ctagIndex;
        manadatorySpacesParser.parse(pc);
        _complCode = completonCodeParser.parse(pc);
        eolParser.parse(pc);
        _body = bodyParser.parse(pc);
    }
}
