package com.ciena.logx.logrecord;

/**
 * Created by gbromfie on 5/6/16.
 */
public class LogRecord {

    private byte[] _buffer;
    private int _length;
    private String _bufferStr;

    public LogRecord(byte[] buffer, int length) {
        _buffer = new byte[length];
        for(int i = 0; i < length; i++) {
            _buffer[i] = buffer[i];
        }
        _length = length;
        _bufferStr = null;
    }

    public byte[] getBuffer() {
        return _buffer;
    }

    public int getLength() {
        return _length;
    }

    @Override
    public String toString() {
        if (_bufferStr == null) {
            StringBuilder bldr = new StringBuilder();
            for(int i = 0; i < _length; i++) {
                bldr.append((char)_buffer[i]);
            }
            _bufferStr = bldr.toString();
        }
        return _bufferStr;
    }
}
