package com.ciena.logx.logfile.ra;

import com.ciena.logx.logfile.LogFileParser;
import com.ciena.logx.logrecord.LogRecord;
import com.grb.parseutils.CharacterList;
import com.grb.parseutils.ParseContext;
import com.grb.parseutils.TextParser;

import java.io.*;
import java.nio.BufferOverflowException;
import java.text.ParseException;
import java.util.HashMap;

/**
 * Created by gbromfie on 5/6/16.
 */
public class RALogFileParser implements LogFileParser {

    static public int LOG_FILE_BUFFER_SIZE = 1000000;

    /**
     * Buffer size for the Log Record
     */
    static public int BUFFER_SIZE = 1000000;
    // The date and time length combined
    final static private int PREMABLE_SIZE = 24;

    static private HashMap<String, Object> _classEnv = new HashMap<String, Object>();

    private HashMap<String, Object> _instanceEnv = new HashMap<String, Object>();
    private String _filename = null;
    private byte[] _buffer = new byte[BUFFER_SIZE];
    private int _bufferIdx = 0;
    private boolean _eof = false;
//    private LogRecord _logRecord = new LogRecord(_buffer);

    private BufferedReader _reader;
    private char[] _fileBuffer = new char[LOG_FILE_BUFFER_SIZE];
    private int _fileBufferLength = 0;
    private int _fileBufferIndex = 0;

    public static final TextParser mandatorySpaceParser = new TextParser()
            .setAllowedChars(CharacterList.NO_CHARS)
            .addAllowedChar(' ')
            .setDelimiterChars(CharacterList.ALL_CHARS)
            .removeDelimeterChar(' ')
            .includeDelimiter(false)
            .setLengths(1, 1)
            .addEOBDelimeter();

    public static final TextParser dateParser = new TextParser()
            .setAllowedChars(CharacterList.NUMBER_CHARS)
            .addAllowedChar('-')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(10, 10);

    public static final TextParser timeParser = new TextParser()
            .setAllowedChars(CharacterList.NUMBER_CHARS)
            .addAllowedChar(':')
            .addAllowedChar(',')
            .setDelimiterChars(CharacterList.WHITESPACE_CHARS)
            .includeDelimiter(false)
            .setLengths(12, 12);

    public RALogFileParser(String filename) throws IOException {
        _filename = filename;
        File fin = new File(filename);
        _reader = new BufferedReader(new FileReader(filename));
    }

    public void dispose() {
        if (_reader != null) {
            try {
                _reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public boolean hasNext() {
        return !_eof;
    }

    @Override
    public LogRecord next() {
        boolean foundFirst = (_bufferIdx > 0);
        try {
            while(true) {
                if (_fileBufferIndex < _fileBufferLength) {
                    while(_fileBufferIndex < _fileBufferLength) {
                        if (_bufferIdx >= BUFFER_SIZE) {
                            throw new BufferOverflowException();
                        }
                        _buffer[_bufferIdx++] = (byte)_fileBuffer[_fileBufferIndex++];
                        if (_bufferIdx >= PREMABLE_SIZE) {
                            ParseContext pc = new ParseContext(_buffer, _bufferIdx - PREMABLE_SIZE, PREMABLE_SIZE);
                            try {
                                dateParser.parse(pc);
                                mandatorySpaceParser.parse(pc);
                                timeParser.parse(pc);
                                mandatorySpaceParser.parse(pc);
                                if (foundFirst) {
                                    LogRecord logRecord = new LogRecord(_buffer, _bufferIdx - PREMABLE_SIZE);
                                    reset();
                                    return logRecord;
                                } else {
                                    foundFirst = true;
                                }
                            } catch (ParseException e) {
                                // no match
                            }
                        }
                    }
                } else {
                    _fileBufferLength = _reader.read(_fileBuffer);
                    _fileBufferIndex = 0;
                    if (_fileBufferLength <= 0) {
                        // EOF
                        break;
                    }
                }
            }
            _eof = true;
            LogRecord logRecord = new LogRecord(_buffer, _bufferIdx);
            return logRecord;
        } catch (IOException e) {
            _eof = true;
            e.printStackTrace();
        }
        return null;
    }

    private void reset() {
        for(int i = 0; i < PREMABLE_SIZE; i++) {
            _buffer[i] = _buffer[_bufferIdx - PREMABLE_SIZE + i];
        }
//        System.out.println("BufferSize = " + _bufferIdx);
        _bufferIdx = PREMABLE_SIZE;
    }
}
