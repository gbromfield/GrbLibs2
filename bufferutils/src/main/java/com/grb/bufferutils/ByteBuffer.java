package com.grb.bufferutils;

import java.io.EOFException;
import java.io.OutputStream;
import java.math.BigInteger;

public class ByteBuffer extends OutputStream {

    final static private short MAX_UBYTE             = 255;
    final static private int MAX_USHORT              = 65535;
    final static private int MAX_UTHRINT             = 16777215;
    final static private long MAX_UINT               = 4294967295L;
    final static private BigInteger MAX_ULONG        = new BigInteger("18446744073709551615");

	final static private boolean ALLOCATION_DOUBLING_ENABLED = true;
    
    protected byte[] mData;
    protected int mOffset;
    protected int mCapacity;
    protected int mReadPosition;
    protected int mWritePosition;
    protected boolean mReadOnly;
    protected int mReadMark;
    protected int mWriteMark;
    
    public ByteBuffer(int capacity) {
        mData = new byte[capacity];
        mOffset = 0;
        mCapacity = capacity;
        mReadPosition = 0;
        mWritePosition = 0;
        mReadOnly = false;
        mReadMark = mReadPosition;
        mWriteMark = mWritePosition;
    }

    public ByteBuffer(byte[] data, int offset) {
        mData = data;
        mOffset = offset;
        mCapacity = data.length;
        mReadPosition = mOffset;
        mWritePosition = data.length;
        mReadOnly = false;
        mReadMark = mReadPosition;
        mWriteMark = mWritePosition;
    }

    public ByteBuffer(ByteArray data) {
        mData = data.getBuffer();
        mOffset = data.getOffset();
        mCapacity = data.getLength();
        mReadPosition = data.getOffset();
        mWritePosition = data.getOffset() + data.getLength();
        mReadOnly = false;
        mReadMark = mReadPosition;
        mWriteMark = mWritePosition;
    }

	/**
	 * Internal use: copy-constructor to allow message cloning
	 */
	public ByteBuffer(ByteBuffer toclone) {
	    mData = new byte[toclone.mData.length];
	    System.arraycopy(toclone.mData, 0, mData, 0, mData.length);
		mOffset = toclone.mOffset;
		mCapacity = toclone.mCapacity;
		mReadPosition = toclone.mReadPosition;
		mWritePosition = toclone.mWritePosition;
		mReadOnly = toclone.mReadOnly;
		mReadMark = toclone.mReadMark;
		mWriteMark = toclone.mWriteMark;
	}
    
    public ByteArray asByteArray() {
        return new ByteArray(mData, mOffset, mWritePosition - mOffset);
    }
    
    public byte[] getBackingArray() {
        return mData;
    }
    
    public int getBackingArrayOffset() {
        return mOffset;
    }
    
    public int getLength() {
        return mWritePosition - mOffset;
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;
    }
    
    public boolean hasRemaining() {
        return (mReadPosition < mWritePosition);
    }
    
    public int remaining() {
        int left = mWritePosition - mReadPosition;
        if (left < 0) {
            return 0;
        }
        return left;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteBuffer) {
            ByteBuffer other = (ByteBuffer)obj;
            return asByteArray().equals(other.asByteArray());
        }
        return false;
    }

    public int readPosition() {
        return mReadPosition;
    }

    public void mark() {
        mReadMark = mReadPosition;
    }
    
    public void reset() {
        mReadPosition = mReadMark;
    }

    public void writeMark() {
        mWriteMark = mWritePosition;
    }
    
    public int getWriteMark() {
        return mWriteMark;
    }

    public void rewind() {
        mReadPosition = mOffset;
    }

    public void clear() {
        rewind();
        mWritePosition = mOffset;
    }

    public byte readByte() throws EOFException {
        if ((mReadPosition + 1) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        return mData[mReadPosition++];
    }

    public short readUByte() throws EOFException {
        if ((mReadPosition + 1) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        return (short)(0x00FF & ((int)mData[mReadPosition++]));
    }

    public short readShort() throws EOFException {
        if ((mReadPosition + 2) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        int b0 = (0x000000FF & ((int)mData[mReadPosition++]));
        int b1 = (0x000000FF & ((int)mData[mReadPosition++])); 
        return (short)(b0 <<8 | b1);
    }

    public char readChar() throws EOFException {
        return (char)readShort();
    }
    
    public int readUShort() throws EOFException {
        if ((mReadPosition + 2) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        int b0 = (0x000000FF & ((int)mData[mReadPosition++]));
        int b1 = (0x000000FF & ((int)mData[mReadPosition++])); 
        return (b0 <<8 | b1);
    }

    public int readUThrint() throws EOFException {
        if ((mReadPosition + 3) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        int b0 = 0x000000FF & mData[mReadPosition++];
        int b1 = 0x000000FF & mData[mReadPosition++];
        int b2 = 0x000000FF & mData[mReadPosition++];
        return (b0 <<16 | b1 <<8 | b2);
    }

    public int readInt() throws EOFException {
        if ((mReadPosition + 4) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        int b0 = 0x000000FF & mData[mReadPosition++];
        int b1 = 0x000000FF & mData[mReadPosition++];
        int b2 = 0x000000FF & mData[mReadPosition++];
        int b3 = 0x000000FF & mData[mReadPosition++];
        return (b0 <<24 | b1 <<16 | b2 <<8 | b3);
    }

    public long readUInt() throws EOFException {
        if ((mReadPosition + 4) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        int b0 = 0x000000FF & mData[mReadPosition++];
        int b1 = 0x000000FF & mData[mReadPosition++];
        int b2 = 0x000000FF & mData[mReadPosition++];
        int b3 = 0x000000FF & mData[mReadPosition++];
        return ((long)b0 <<24 | (long)b1 <<16 | (long)b2 <<8 | (long)b3);
    }

    public long readLong() throws EOFException {
        if ((mReadPosition + 8) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        int b0 = 0x000000FF & mData[mReadPosition++];
        int b1 = 0x000000FF & mData[mReadPosition++];
        int b2 = 0x000000FF & mData[mReadPosition++];
        int b3 = 0x000000FF & mData[mReadPosition++];
        int b4 = 0x000000FF & mData[mReadPosition++];
        int b5 = 0x000000FF & mData[mReadPosition++];
        int b6 = 0x000000FF & mData[mReadPosition++];
        int b7 = 0x000000FF & mData[mReadPosition++];
        return ((long)b0 <<56 | (long)b1 <<48 | (long)b2 <<40 | 
                (long)b3 <<32 | (long)b4 <<24 | (long)b5 <<16 | 
                (long)b6 <<8 | (long)b7);
    }

    public BigInteger readULong() throws EOFException {
        if ((mReadPosition + 8) > mWritePosition) {
            throw new EOFException("unexpected end of buffer reached");
        }
        byte[] bytes = new byte[9];
        bytes[0] = 0x00; 
        for(int i = 0; i < 8; i++) {
            bytes[i+1] = (byte)(mData[mReadPosition++] & 0x000000FF);
        }
        return new BigInteger(bytes);
    }

    public float readFloat() throws EOFException {
        int value = readInt();
        return Float.intBitsToFloat(value);
    }

    public double readDouble() throws EOFException {
        long value = readLong();
        return Double.longBitsToDouble(value);
    }

    public ByteArray readBytes(int length) throws EOFException {
        if ((mReadPosition + length) > mWritePosition) {
            throw new EOFException("end of buffer reached");
        }
        ByteArray ba = new ByteArray(mData, mReadPosition, length);
        mReadPosition += length;
        return ba;
    }
     
    public void writeByte(byte b) {
        realloc(1);
        mData[mWritePosition++] = b;
    }

    public void writeByteUnsafe(byte b) {
        mData[mWritePosition++] = b;
    }

    public void writeUByte(short s) {
        if ((s < 0) || (s > MAX_UBYTE)) {
            throw new IllegalArgumentException("unsigned byte value out of range (" + s + ")");
        }
        writeByte((byte)(s & 0xFF));
    }

    public void writeByte(int position, byte b) {
        mData[position] = b;
    }

    public void writeUByte(int position, short s) {
        if ((s < 0) || (s > MAX_UBYTE)) {
            throw new IllegalArgumentException("unsigned byte value out of range (" + s + ")");
        }
        mData[position] = (byte)(s & 0xFF);
    }

    public void writeBytes(byte[] src) {
        realloc(src.length);
        System.arraycopy(src, 0, mData, mWritePosition, src.length);
        mWritePosition += src.length;
    }

    public void writeBytes(byte[] src, int offset, int length) {
        realloc(length);
        System.arraycopy(src, offset, mData, mWritePosition, length);
        mWritePosition += length;
    }

    public void writeBytes(ByteArray src) {
        realloc(src.getLength());
        System.arraycopy(src.getBuffer(), src.getOffset(), mData, mWritePosition, src.getLength());
        mWritePosition += src.getLength();
    }

    public void writeChar(char c) {
        writeShort((short)c);
    }

    public void writeChar(int position, char c) {
        writeShort(position, (short)c);
    }

    public void writeShort(short s) {
        realloc(2);
        mData[mWritePosition++] = (byte)((s & 0x0000FF00L) >>8);
        mData[mWritePosition++] = (byte)(s & 0x000000FFL);
    }

    public void writeShort(int position, short s) {
        mData[position] = (byte)((s & 0x0000FF00L) >>8);
        mData[position+1] = (byte)(s & 0x000000FFL);
    }

    public void writeUShort(int i) {
        if ((i < 0) || (i > MAX_USHORT)) {
            throw new IllegalArgumentException("unsigned short value out of range (" + i + ")");
        }
        realloc(2);
        mData[mWritePosition++] = (byte)((i & 0x0000FF00L) >>8);
        mData[mWritePosition++] = (byte)(i & 0x000000FFL);
    }

    public void writeUShort(int position, int i) {
        if ((i < 0) || (i > MAX_USHORT)) {
            throw new IllegalArgumentException("unsigned short value out of range (" + i + ")");
        }
        mData[position] = (byte)((i & 0x0000FF00L) >>8);
        mData[position+1] = (byte)(i & 0x000000FFL);
    }

    public void writeUThrint(int i) {
        if ((i < 0) || (i > MAX_UTHRINT)) {
            throw new IllegalArgumentException("unsigned three byte value out of range (" + i + ")");
        }
        realloc(3);
        mData[mWritePosition++] = (byte)((i & 0x00FF0000L) >>16);
        mData[mWritePosition++] = (byte)((i & 0x0000FF00L) >>8);
        mData[mWritePosition++] = (byte)(i & 0x000000FFL);
    }

    public void writeUThrint(int position, int i) {
        if ((i < 0) || (i > MAX_UTHRINT)) {
            throw new IllegalArgumentException("unsigned three byte value out of range (" + i + ")");
        }
        mData[position] = (byte)((i & 0x00FF0000L) >>16);
        mData[position+1] = (byte)((i & 0x0000FF00L) >>8);
        mData[position+2] = (byte)(i & 0x000000FFL);
    }

    public void writeInt(int i) {
        realloc(4);
        mData[mWritePosition++] = (byte)((i & 0xFF000000L) >>24);
        mData[mWritePosition++] = (byte)((i & 0x00FF0000L) >>16);
        mData[mWritePosition++] = (byte)((i & 0x0000FF00L) >>8);
        mData[mWritePosition++] = (byte)(i & 0x000000FFL);
    }

    public void writeInt(int position, int i) {
        mData[position] = (byte)((i & 0xFF000000L) >>24);
        mData[position+1] = (byte)((i & 0x00FF0000L) >>16);
        mData[position+2] = (byte)((i & 0x0000FF00L) >>8);
        mData[position+3] = (byte)(i & 0x000000FFL);
    }

    public void writeUInt(long l) {
        if ((l < 0) || (l > MAX_UINT)) {
            throw new IllegalArgumentException("unsigned int value out of range (" + l + ")");
        }
        realloc(4);
        mData[mWritePosition++] = (byte)((l & 0xFF000000L) >>24);
        mData[mWritePosition++] = (byte)((l & 0x00FF0000L) >>16);
        mData[mWritePosition++] = (byte)((l & 0x0000FF00L) >>8);
        mData[mWritePosition++] = (byte)(l & 0x000000FFL);
    }

    public void writeUInt(int position, long l) {
        if ((l < 0) || (l > MAX_UINT)) {
            throw new IllegalArgumentException("unsigned int value out of range (" + l + ")");
        }
        mData[position] = (byte)((l & 0xFF000000L) >>24);
        mData[position+1] = (byte)((l & 0x00FF0000L) >>16);
        mData[position+2] = (byte)((l & 0x0000FF00L) >>8);
        mData[position+3] = (byte)(l & 0x000000FFL);
    }

    public void writeLong(long l) {
        realloc(8);
        mData[mWritePosition++] = (byte)((l & 0xFF00000000000000L) >>56);
        mData[mWritePosition++] = (byte)((l & 0x00FF000000000000L) >>48);
        mData[mWritePosition++] = (byte)((l & 0x0000FF0000000000L) >>40);
        mData[mWritePosition++] = (byte)((l & 0x000000FF00000000L) >>32);
        mData[mWritePosition++] = (byte)((l & 0x00000000FF000000L) >>24);
        mData[mWritePosition++] = (byte)((l & 0x0000000000FF0000L) >>16);
        mData[mWritePosition++] = (byte)((l & 0x000000000000FF00L) >>8);
        mData[mWritePosition++] = (byte)(l & 0x00000000000000FFL);
    }

    public void writeLong(int position, long l) {
        mData[position] = (byte)((l & 0xFF00000000000000L) >>56);
        mData[position+1] = (byte)((l & 0x00FF000000000000L) >>48);
        mData[position+2] = (byte)((l & 0x0000FF0000000000L) >>40);
        mData[position+3] = (byte)((l & 0x000000FF00000000L) >>32);
        mData[position+4] = (byte)((l & 0x00000000FF000000L) >>24);
        mData[position+5] = (byte)((l & 0x0000000000FF0000L) >>16);
        mData[position+6] = (byte)((l & 0x000000000000FF00L) >>8);
        mData[position+7] = (byte)(l & 0x00000000000000FFL);
    }

    public void writeULong(BigInteger bi) {
        if ((bi.compareTo(BigInteger.ZERO) < 0) || (bi.compareTo(MAX_ULONG) > 0)) {
            throw new IllegalArgumentException("unsigned long value out of range (" + bi + ")");
        }
        realloc(8);
        byte[] bytes = bi.toByteArray();
        int len = bytes.length;
        int offset = 0;
        if (bytes.length > 8) {
            len = 8;
            offset = bytes.length - 8;
        }
        for(int i = 0; i < 8; i++) {
            if (i < (8 - len)) {
                mData[mWritePosition++] = (byte)0;
            } else {
                mData[mWritePosition++] = bytes[offset + (i-(8-len))];
            }
        }
    }

    public void writeULong(int position, BigInteger bi) {
        if ((bi.compareTo(BigInteger.ZERO) < 0) || (bi.compareTo(MAX_ULONG) > 0)) {
            throw new IllegalArgumentException("unsigned long value out of range (" + bi + ")");
        }
        byte[] bytes = bi.toByteArray();
        int len = bytes.length;
        int offset = 0;
        if (bytes.length > 8) {
            len = 8;
            offset = bytes.length - 8;
        }
        int pos = position;
        for(int i = 0; i < 8; i++) {
            if (i < (8 - len)) {
                mData[pos++] = (byte)0;
            } else {
                mData[pos++] = bytes[offset + (i-(8-len))];
            }
        }
    }

    public void writeFloat(float f) {
        writeInt(Float.floatToIntBits(f));
    }

    public void writeFloat(int position, float f) {
        writeInt(position, Float.floatToIntBits(f));
    }

    public void writeDouble(double d) {
        writeLong(Double.doubleToLongBits(d));
    }

    public void writeDouble(int position, double d) {
        writeLong(position, Double.doubleToLongBits(d));
    }

    public void skipRead(int num) throws EOFException {
        if (remaining() < num) {
            throw new EOFException("unexpected end of buffer reached");
        }
        mReadPosition += num;
    }
    
    public void skipWrite(int num) {
        realloc(num);
        mWritePosition += num;
    }

    public void skipToEnd() {
        mReadPosition = mWritePosition;
    }

    /**
     * Reallocates the byte array if more space is needed.
     */
    public void realloc(int quantum) {
        int newCapacity = mWritePosition - mOffset + quantum;
        if ((newCapacity > mCapacity) || mReadOnly) {
            if (newCapacity <= mCapacity) {
                newCapacity = mCapacity;
            } else {
                if (ALLOCATION_DOUBLING_ENABLED) {
                    newCapacity = Math.max(newCapacity, mCapacity * 2);                
                }
            }
            byte[] newData = new byte[newCapacity];
            System.arraycopy(mData, mOffset, newData, 0, mCapacity);
            mData = newData;
            mReadPosition = mReadPosition - mOffset;
            mWritePosition = mWritePosition - mOffset;
            mReadMark = mReadMark - mOffset;
            mWriteMark = mWriteMark - mOffset;
            mOffset = 0;
            mCapacity = newCapacity;
            mReadOnly = false;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        writeBytes(b, off, len);
    }

    @Override
    public void write(byte[] b) {
        writeBytes(b);
    }

    @Override
    public void write(int b) {
        writeByte((byte)b);
    }
}
