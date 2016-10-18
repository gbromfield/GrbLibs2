package com.grb.tl1;

import java.nio.ByteBuffer;
import java.text.ParseException;

import com.grb.parseutils.CharacterList;
import com.grb.parseutils.ParseContext;
import com.grb.parseutils.TextParser;

abstract public class TL1AckMessage extends TL1OutputMessage {

    public static final int PREAMBLE_FINGERPRINT_SIZE = 3;

    public static final byte[] PROLOGUE = "\r\n<".getBytes();

    private static final int INITIAL_BUFFER_SIZE = 10;

    private static final TextParser ackCodeParser = new TextParser()
            .setAllowedChars(CharacterList.ALPHABETIC_MINUS_WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(2, 2);

    protected String _ctag;
    protected int _ctagIndex;
    protected int _ctagLength;

    protected TL1AckMessage(String ackCode, String ctag) throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
        _ctag = ctag;
        _ctagIndex = 0;
        _ctagLength = 0;
        String ackMsg = ackCode + " " + ctag + "\r\n<";
        _buffer.writeBytes(ackMsg.getBytes());
    }

    protected TL1AckMessage() throws TL1MessageMaxSizeExceededException {
        super(INITIAL_BUFFER_SIZE);
        _ctag = null;
    }

	public boolean parse(ByteBuffer readBuffer) throws ParseException {
        while(readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            _buffer.writeByte(b);
            if (_buffer.getLength() >= getMessageStartIdx()) {
                if (_buffer.getLength() > _messageStartIdx) {
                    if (b == '<') {
                        parseTL1();
                        return true;
                    }
                }
            }
        }
        return false;
	}

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

    abstract public String getAckCode();
    
    private void parseTL1() throws ParseException {
        ParseContext pc = new ParseContext(_buffer.getBackingArray(), _messageStartIdx, _buffer.getLength() - _messageStartIdx);
        ackCodeParser.parse(pc, 2);
        manadatorySpacesParser.parse(pc);
        _ctagIndex = pc.mark;
        _ctag = ctagParser.parse(pc);
        _ctagLength = pc.mark - _ctagIndex;

    }
}
