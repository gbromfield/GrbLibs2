package com.grb.bufferutils;

import java.util.ArrayList;

/**
 * Basic Format %[range and separator]format
 * <li> optional range can be in the form "1" or "3-9".
 * <li> separator can be any string other than with characters ${}[] etc...
 * <li> format is a single character and can be one of x (hex), o (oct),
 * d (dec), y (hex index), p (oct index), i (dec index), s (string).
 * 
 * <h3>Examples:</h3>
 * "%02d" - applies the same 02d format to all bytes in the array.
 * "%02i - applies the 02d format to the index into the array.
 * %[0]02d - applies the 02d format to the zeroth element in the array.
 * %[0-7]02d - applies the 02d format to the first 8 elements in the array.
 * %[,]02d - same as %02d but uses a comma as a separator.
 * %[0-7 ]02d - applies the 02d format to the first 8 elements (separated by a space) in the array.
 * %04y  %[0-7 ]02x %[8-15 ]02x  %[0-7]s %[8-15]s - this is the wireshark format. 4 digit index followed by 16 hex digits and 16 characters
 */
public class ByteArrayFormatter {
    
	final static public String WIRESHARK_FORMAT = "%04y  %[0-7 ]02x %[8-15 ]02x  %[0-7]s %[8-15]s";
	final static public String BASIC_HEX_FORMAT = "%[ ]02x";
    final static public String BASIC_DEC_FORMAT = "%[ ]02d";
    final static public String BASIC_OCT_FORMAT = "%[ ]02o";
    final static public String BASIC_STR_FORMAT = "%s";
	
    static public String DEFAULT_FORMAT = WIRESHARK_FORMAT;
    static public int DEFAULT_LIMIT_SIZE = 1024;
    static public LimitType DEFAULT_LIMIT_TYPE = LimitType.BYTES;
    static public char DEFAULT_UNPRINTABLE_CHAR = '.';
    static public String DEFAULT_MORE_DATA_STRING = " <...>";
    
	public enum LimitType {
		BYTES,
		ROWS
	}
	
    private class RangeElement {
        public String original = null;
        public Integer min = null;
        public Integer max = null;
        public String separator = null;
        
        public String toString() {
            return String.format("%s min=%d, max=%d, separator=\"%s\"", original, min, max, separator);
        }
    }
    
    private class FormatElement {
        public String original = null;
        public RangeElement range = null;
        public String format = null;
        public boolean index = false;

        public String toString() {
            return String.format("%s range=(%s), format=%s, index=%b", original, range, format, index);
        }
    }
    
    private ArrayList<FormatElement> mFormatElements;
    private String mFormat;
    private String mOutFormat;
    private int mLineSize;
    private char mUnprintableChar;
    private Integer mLimit;
    private LimitType mLimitType;
    private String mMoreData;
    
    public ByteArrayFormatter(String format) {
    	this(format, null, null);
    }

    public ByteArrayFormatter(String format, Integer limit, LimitType limitType) {
        mFormatElements = new ArrayList<ByteArrayFormatter.FormatElement>();
        mFormat = format;
        mOutFormat = null;
        mLineSize = -1;
        mUnprintableChar = DEFAULT_UNPRINTABLE_CHAR;
        if (limit == null) {
            mLimit = DEFAULT_LIMIT_SIZE;
        } else {
            mLimit = limit;
        }
        if (limitType == null) {
            mLimitType = DEFAULT_LIMIT_TYPE;
        } else {
            mLimitType = limitType;
        }
        mMoreData = DEFAULT_MORE_DATA_STRING;
        parseFormat();
        validateFormat();
    }

    synchronized public void setFormat(String format) {
        mFormat = format;
        parseFormat();
        validateFormat();
    }
    
    synchronized public String getFormat() {
        return mFormat;
    }
    
    synchronized public void setUnprintableChar(char c) {
    	mUnprintableChar = c;
    }
    
    synchronized public char getUnprintableChar() {
    	return mUnprintableChar;
    }
    
    synchronized public void setLimit(Integer limit, LimitType limitType) {
    	mLimit = limit;
    	mLimitType = limitType;
    }
    
    synchronized public Integer getLimit() {
    	return mLimit;
    }
    
    synchronized public LimitType getLimitType() {
    	return mLimitType;
    }
    
    synchronized public void setMoreDataString(String str) {
    	mMoreData = str;
    }
    
    synchronized public String getMoreDataString() {
    	return mMoreData;
    }
    
    synchronized public String format(java.nio.ByteBuffer buffer) {
        return format(buffer.array(), buffer.arrayOffset(), buffer.limit() - buffer.arrayOffset());
    }
    
    synchronized public String format(byte[] data) {
        return format(data, 0, data.length);
    }
    
    synchronized public String format(byte[] data, int offset, int length) {
        if (mFormatElements.size() == 0) {
            return mFormat;
        }
        StringBuilder result = new StringBuilder();
        int lineSize = Math.min(mLineSize, length);
        int lineOffset = 0;
        int numRows = 0;
        boolean atLimit = false;
        int byteCount = 0;
        while(!atLimit) {
            String outFormat = new String(mOutFormat);
            for(int i = 0; i < mFormatElements.size(); i++) {
                FormatElement elem = mFormatElements.get(i);
                StringBuilder elemBldr = new StringBuilder();
                if (elem.index) {
                    elemBldr.append(String.format(elem.format, lineOffset));
                    byteCount++;
                    atLimit = isAtLimit(byteCount, LimitType.BYTES);
                } else {
                    int min = 0;
                    int max = lineSize - 1;
                    if (elem.range != null) {
                        if (elem.range.min != null) {
                            min = elem.range.min;
                        }
                        if (elem.range.max != null) {
                            max = elem.range.max;
                        }
                    }
                    int index2 = min;
                    while(index2 <= max) {
                        if ((index2 > 0) && (elem.range != null) && (elem.range.separator != null)) {
                            elemBldr.append(elem.range.separator);
                        }
                        if ((lineOffset+index2) < length) {
                            if (elem.format.endsWith("s")) {
                                byte datum = data[offset+lineOffset+index2];
                                if ((datum < 32) || (datum > 126)) {
                                    elemBldr.append(String.format(elem.format, mUnprintableChar));
                                } else {
                                    elemBldr.append(String.format(elem.format, (char)datum));
                                }
                            } else {
                                elemBldr.append(String.format(elem.format, data[offset+lineOffset+index2]));
                            }
                        } else {
                            elemBldr.append(asSpaces(elem.format, (byte)0));
                        }
                        index2++;
                        byteCount++;
                        atLimit = isAtLimit(byteCount, LimitType.BYTES);
                        if (atLimit) {
                            break;
                        }
                    }
                }
                outFormat = replace(outFormat, String.format("{$%d}", i), elemBldr.toString());
                if (atLimit) {
                	outFormat = cleanup(outFormat);
                    break;
                }
            }
            if ((result.length() > 0) && (outFormat.length() > 0)) {
                result.append("\r\n");
            }
            result.append(outFormat);
            lineOffset += lineSize;
            if (atLimit) {
            	result.append(mMoreData);
            }
            if (lineOffset >= length) {
                break;
            }
            numRows++;
            if (!atLimit) {
                atLimit = isAtLimit(numRows, LimitType.ROWS);
                if (atLimit) {
                	result.append(mMoreData);
                }
            }
        }
        return result.toString();
    }
    
	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		bldr.append("fmt=\"");
		bldr.append(mFormat);
		bldr.append("\", outFmt=\"");
		bldr.append(mOutFormat);
        bldr.append("\", maxIdx=");
        bldr.append(mLineSize);
		bldr.append(", elems={");
		for(int i = 0; i < mFormatElements.size(); i++) {
		    if (i > 0) {
		        bldr.append(",");
		    }
		    bldr.append("(");
		    bldr.append(mFormatElements.get(i));
		    bldr.append(")");
		}
		bldr.append("}");
		return bldr.toString();
	}
        
    private void parseFormat() {
        int formatStart = -1;
        int rangeStart = -1;
        StringBuilder minStr = null;;
        StringBuilder maxStr = null;
        StringBuilder separatorStr = null;
        StringBuilder current = null;
        StringBuilder format = null;
        boolean isIndex = false;
        RangeElement rangeElem = null;
        StringBuilder bldr = new StringBuilder();
        int length = mFormat.length();
        int index = 0;
        while(index < length) {
            if (rangeStart != -1) {
                if (mFormat.charAt(index) == ']') {
                    rangeElem = new RangeElement();
                    rangeElem.original = mFormat.substring(rangeStart, index+1);
                    if (minStr.length() > 0) {
                        rangeElem.min = Integer.parseInt(minStr.toString());
                    }
                    if (maxStr.length() == 0) {
                        rangeElem.max = rangeElem.min;
                    } else {
                        rangeElem.max = Integer.parseInt(maxStr.toString());
                    }
                    if (rangeElem.max != null) {
                        mLineSize = Math.max(mLineSize, rangeElem.max + 1);
                    }
                    if (separatorStr.length() > 0) {
                        rangeElem.separator = separatorStr.toString();
                    }
                    if ((rangeElem.min != null) && (rangeElem.max != null) && (rangeElem.max < rangeElem.min)) {
                        throw new IllegalArgumentException(String.format("Error processing range %s, min value %d is greater than max value %d", 
                                rangeElem.original, rangeElem.min, rangeElem.max));
                    }
                    rangeStart = -1;
                } else {
                    if (Character.isDigit(mFormat.charAt(index))) {
                        current.append(mFormat.charAt(index));
                    } else if (mFormat.charAt(index) == '-') {
                        current = maxStr;
                    } else {
                        current = separatorStr;
                        current.append(mFormat.charAt(index));
                    }
                }
            } else if (formatStart != -1) {
                if (mFormat.charAt(index) == '%') {
                    throw new IllegalArgumentException(String.format("Error processing format \"%s\", embedded percent sign", 
                            mFormat.substring(formatStart, index+1)));
                }
                if (mFormat.charAt(index) == '[') {
                    rangeStart = index;
                    minStr = new StringBuilder();
                    maxStr = new StringBuilder();
                    separatorStr = new StringBuilder();
                    current = minStr;
                } else if ((mFormat.charAt(index) == 'x') ||
                           (mFormat.charAt(index) == 'o') ||
                           (mFormat.charAt(index) == 'd') ||
                           (mFormat.charAt(index) == 's') ||
                           (mFormat.charAt(index) == 'y') ||
                           (mFormat.charAt(index) == 'p') ||
                           (mFormat.charAt(index) == 'i')) {
                    if (mFormat.charAt(index) == 'y') {
                        format.append('x');
                        isIndex = true;
                    } else if (mFormat.charAt(index) == 'p') {
                        format.append('o');
                        isIndex = true;
                    } else if (mFormat.charAt(index) == 'i') {
                        format.append('d');
                        isIndex = true;
                    } else {
                        format.append(mFormat.charAt(index));
                    }
                    if (isIndex && (rangeElem != null)) {
                        throw new IllegalArgumentException(String.format("Error processing format \"%s\", ranges not allowed in index formatting \"%s\"", 
                                mFormat.substring(formatStart, index+1), format.toString()));
                    }
                    FormatElement elem = new FormatElement();
                    elem.original = mFormat.substring(formatStart, index+1);
                    elem.format = format.toString();
                    elem.range = rangeElem;
                    elem.index = isIndex;
                    bldr.append(String.format("{$%d}", mFormatElements.size()));
                    mFormatElements.add(elem);
                    formatStart = -1;
                } else {
                    format.append(mFormat.charAt(index));
                }
            } else {
                if (mFormat.charAt(index) == '%') {
                    formatStart = index;
                    format = new StringBuilder();
                    format.append('%');
                    isIndex = false;
                    rangeElem = null;
                } else {
                    bldr.append(mFormat.charAt(index));
                }
            }
            index++;
        }
        if (mLineSize == -1) {
            mLineSize = Integer.MAX_VALUE;
        }
        mOutFormat = bldr.toString();
    }
              
    private void validateFormat() {
        // must all have range or one with no range
        Boolean hasRange = null;
        for(int i = 0; i < mFormatElements.size(); i++) {
            FormatElement elem = mFormatElements.get(i);
            if (!elem.index) {
                if ((elem.range == null) || (elem.range.min == null)) {
                    if (hasRange == null) {
                        hasRange = false;
                    } else {
                        if (hasRange) {
                            throw new IllegalArgumentException(String.format("Illegal format specifier \"%s\" - all formats must have a range or all none", elem.format));
                        }
                    }
                } else {
                    if (hasRange == null) {
                        hasRange = true;
                    } else {
                        if (!hasRange) {
                            throw new IllegalArgumentException(String.format("Illegal format specifier \"%s\" - all formats must have a range or all none", elem.format));
                        }
                    }
                }
            }
        }
    }
    
    private String replace(String str, String token, String replacement) {
    	int index = str.indexOf(token);
    	if (index == -1) {
    		return str;
    	}
    	return str.substring(0, index) + replacement + str.substring(index + token.length());
    }
    
    private String cleanup(String str) {
    	StringBuilder bldr = new StringBuilder(str);
    	int endIdx = 0;
    	int startIdx = 0;
    	boolean found = false;
    	while((startIdx = bldr.indexOf("{$", endIdx)) != -1) {
    		endIdx = bldr.indexOf("}", startIdx);
    		if (endIdx == -1) {
    			endIdx = startIdx + 1;
    		} else {
    			bldr.delete(startIdx,  endIdx+1);
    			found = true;
    			endIdx = startIdx;
    		}    		
    	}
    	if (!found) {
    		return str;
    	}
        int index = bldr.length() - 1;
        while(Character.isWhitespace(bldr.charAt(index))) {
        	bldr.deleteCharAt(index);
        	index--;
        }
    	return bldr.toString();
    }
    
    private String asSpaces(String fmt, byte datum) {
    	StringBuilder bldr = new StringBuilder();
		String tmp = String.format(fmt, datum);
		for(int i = 0; i < tmp.length(); i++) {
			bldr.append(" ");
		}
		return bldr.toString();
    }
    
    private boolean isAtLimit(int value, LimitType type) {
        if (type.equals(mLimitType)) {
            return (value >= mLimit);
        }
        return false;
    }
    
    private static void test(String format, byte[] data, String expected) {
        StringBuilder bldr = new StringBuilder();
        ByteArrayFormatter fmter = new ByteArrayFormatter(format);
        bldr.append("fmt=\"");
        bldr.append(format);
        bldr.append("\", data=[");
        for(int i = 0; i < data.length; i++) {
            if (i > 0) {
                bldr.append(",");
            }
            bldr.append(data[i]);
        }
        bldr.append("], expected=\"");
        bldr.append(expected);
        String out = fmter.format(data);
        if (expected.equals(out)) {
            bldr.append("\" - SUCCESS");
            System.out.println(bldr.toString());
        } else {
            bldr.append("\" - FAILURE got \"");
            bldr.append(out);
            bldr.append("\"");
            System.err.println(bldr.toString());
        }
    }
    
    public static void main(String[] args) {
        ByteArrayFormatter.DEFAULT_LIMIT_SIZE = 2;
        ByteArrayFormatter.DEFAULT_LIMIT_TYPE = LimitType.ROWS;
        ByteArrayFormatter.test("%04y  %[0-3 ]02x  %[0-3]s", new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "0000  30 31 32 33  0123\r\n0004  34 35 36 37  4567 <...>");
        ByteArrayFormatter.DEFAULT_LIMIT_SIZE = 100;
        ByteArrayFormatter.DEFAULT_LIMIT_TYPE = LimitType.BYTES;
        ByteArrayFormatter.test("%04y  %[0-3 ]02x  %[0-3]s", new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "0000  30 31 32 33  0123\r\n0004  34 35 36 37  4567\r\n0008  38 39 40 41  89@A\r\n000c  42 43 44 45  BCDE");
        ByteArrayFormatter.test("%04y  %[0-2 ]02x  %[0-2]s", new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "0000  30 31 32  012\r\n0003  33 34 35  345\r\n0006  36 37 38  678\r\n0009  39 40 41  9@A\r\n000c  42 43 44  BCD\r\n000f  45        E  ");
        ByteArrayFormatter.DEFAULT_LIMIT_SIZE = 5;
        ByteArrayFormatter.DEFAULT_LIMIT_TYPE = LimitType.BYTES;
        ByteArrayFormatter.test("%s", new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "01234 <...>");
        ByteArrayFormatter.test(WIRESHARK_FORMAT, new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "0000  30 31 32 33 <...>");
        ByteArrayFormatter.DEFAULT_LIMIT_SIZE = 100;
        ByteArrayFormatter.DEFAULT_LIMIT_TYPE = LimitType.BYTES;
        ByteArrayFormatter.test("%s", new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "0123456789@ABCDE");
        ByteArrayFormatter.test(WIRESHARK_FORMAT, new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45}, "0000  30 31 32 33 34 35 36 37  38 39 40 41 42 43 44 45  01234567 89@ABCDE");
        ByteArrayFormatter.test("%[0]s", new byte[] {0x0}, ".");
        ByteArrayFormatter.test("%[0]s", new byte[] {0x55}, "U");
        ByteArrayFormatter.test("%[0]02x %[1]02x %[0]s", new byte[] {0x55}, "55    U");
        ByteArrayFormatter.test("%[0]02x %[1]02x %[0]s %[1]s", new byte[] {0x55}, "55    U  ");
        ByteArrayFormatter.test("%[0]02x %[1]02x %[0]s%[1]s", new byte[] {0x55}, "55    U ");
        ByteArrayFormatter.test("%[0]02x %[1]02x %[0]s%[1]s", new byte[] {0x01}, "01    . ");
        ByteArrayFormatter.test("%04x", new byte[] {}, "");
        ByteArrayFormatter.test("%04y", new byte[] {}, "0000");
        ByteArrayFormatter.test("%04y %[ ]02x", new byte[] {}, "0000 ");
        ByteArrayFormatter.test("%04y %[ ]02x", new byte[] {0x03, 0x04, 0x05}, "0000 03 04 05");
        ByteArrayFormatter.test("%04y %02x", new byte[] {0x03, 0x04, 0x05}, "0000 030405");
        ByteArrayFormatter.test("%[1]04x", new byte[] {}, "    ");
        ByteArrayFormatter.test("%[1-2]04x", new byte[] {}, "        ");
        ByteArrayFormatter.test("%04y", new byte[] {0x00}, "0000");
        ByteArrayFormatter.test("%04i", new byte[] {0x00}, "0000");
        ByteArrayFormatter.test("%02x", new byte[] {0x55}, "55");
        ByteArrayFormatter.test("%02x", new byte[] {0x3f}, "3f");
        ByteArrayFormatter.test("%02d", new byte[] {0x3f}, "63");
        ByteArrayFormatter.test("%s", new byte[] {0x3f}, "?");
        ByteArrayFormatter.test("%2s", new byte[] {0x3f}, " ?");
        ByteArrayFormatter.test("[%2s]", new byte[] {0x3f}, "[ ?]");
        ByteArrayFormatter.test("[%2s]", new byte[] {0x3f, 0x3c}, "[ ? <]");
        ByteArrayFormatter.test("[%[,]s]", new byte[] {0x3f, 0x3c}, "[?,<]");
        ByteArrayFormatter.test("gaga", new byte[] {}, "gaga");
        ByteArrayFormatter.test("", new byte[] {}, "");
    }
}
