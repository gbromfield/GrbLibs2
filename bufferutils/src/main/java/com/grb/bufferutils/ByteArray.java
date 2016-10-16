package com.grb.bufferutils;

/**
 * byte[] implementation with offset and length parameters.
 * This class does not copy the reference array given on
 * construction. This is useful when you want a read-only 
 * reference of a byte[] into an existing larger byte[].
 */
public class ByteArray {
    protected byte[] mBuffer;
    protected int mOffset;
    protected int mLength;
    protected int mHash;
    
    /**
     * Create a byte array that spans the entire length of the given array.
     * 
     * @param buffer Reference byte[]
     */
    public ByteArray(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    /**
     * Create a byte array that spans the reference array length bytes
     * from the offset.
     * 
     * @param buffer Reference byte[]
     * @param offset Offset to start of data
     * @param length Length of the data from the offset
     */
    public ByteArray(byte[] buffer, int offset, int length) {
        validate(buffer, offset, length);
        mBuffer = buffer;
        mOffset = offset;
        mLength = length;
        mHash = 0;
    }

    /**
     * Get the reference backing array.
     * 
     * @return The reference backing array.
     */
    public byte[] getBuffer() {
        return mBuffer;
    }
    
    /**
     * Get the length of the data in the backing array.
     * 
     * @return The length of the data in the backing array.
     */
    public int getLength() {
        return mLength;
    }

    /**
     * Get the offset of the data in the backing array.
     * 
     * @return The offset of the data in the backing array.
     */
    public int getOffset() {
        return mOffset;
    }

    /**
     * Returns a deep copy of the data in the byte array with the offset
     * justified to zero.
     * 
     * @return A deep copy of the data.
     */
    public byte[] asBytes() {
        if ((mOffset == 0) && (mLength == mBuffer.length)) {
            return mBuffer;
        }
        byte[] ba = new byte[mLength];
        System.arraycopy(mBuffer, mOffset, ba, 0, mLength);
        return ba;
    }
    
    @Override
    public int hashCode() {
        int h = mHash;
        if (h == 0) {
            for (int i = 0; i < mLength; i++) {
                h = 31*h + mBuffer[mOffset + i];
            }
            mHash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteArray) {
            ByteArray other = (ByteArray)obj;
            if (mLength != other.mLength) {
                return false;
            }
            for(int i = 0; i < mLength; i++) {
                if (mBuffer[mOffset+i] != other.mBuffer[other.mOffset+i]) {
                    return false;
                }
            }
            return true;
        } else if (obj instanceof byte[]) {
            byte[] other = (byte[])obj;
            if (mLength != other.length) {
                return false;
            }
            for(int i = 0; i < mLength; i++) {
                if (mBuffer[mOffset+i] != other[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (mBuffer == null) {
            return "null";
        }
        StringBuilder bldr = new StringBuilder();
        int len = mLength;
        if (mLength > 100) {
            len = 100;
        }
        for(int i = 0; i < len; i++) {
            if (i == 0) {
                bldr.append("[");
            }
            bldr.append(mBuffer[mOffset+i]);
            if (i == (len-1)) {
                bldr.append("]");
            } else {
                bldr.append(",");
            }
        }
        bldr.append(" (offset=");
        bldr.append(mOffset);
        bldr.append(", length=");
        bldr.append(mLength);
        bldr.append(")");
        return bldr.toString();
    }
    
    private void validate(byte[] buffer, int offset, int length) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null");
        }
        if (length > 0) {
            if ((offset < 0) || (offset >= buffer.length)) {
                throw new IllegalArgumentException("illegal offset value - " + offset);
            }
            if ((length < 0) || ((offset + length) > buffer.length)) {
                throw new IllegalArgumentException("illegal offset/length, offset=" + offset + ", length=" + length);
            }
        }
    }
}
