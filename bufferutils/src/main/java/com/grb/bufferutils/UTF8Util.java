/*
 * UTF8Util.java
 * $Id: UTF8Util.java 10833 2008-11-05 21:52:14Z gbromfield $
 * 
 * Copyright 2004-2005 Solace Systems, Inc.  All rights reserved.
 */
package com.grb.bufferutils;



public class UTF8Util {
    final static char [] DigitTens = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ; 

    final static char [] DigitOnes = { 
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;
    
    final static char[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };
    
    final static byte[] numbers = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0,
        0, 10,11,12,13,14,15,0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 10,11,12,13,14,15,0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        };

    public static int toUTF8(byte[] outputBuffer, short number) {
        return toUTF8(outputBuffer, (long)number);
    }
    
    public static int toUTF8(byte[] outputBuffer, int number) {
        return toUTF8(outputBuffer, (long)number);
    }
    
    /**
     * Encode the <code>number</code> into UTF8
     * 
     * @param outputBuffer buffer for the encoded number
     * @param number The number to be encoded
     * @return The offset of the <code>outputBuffer</code> the encoded string
     */
    public static int toUTF8(byte[] outputBuffer, long number) {
        long q;
        int r;
        int charPos = outputBuffer.length;
        char sign = 0;

        if (number < 0) {
            sign = '-';
            number = -number;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (number > Integer.MAX_VALUE) { 
            q = number / 100;
            r = (int)(number - ((q << 6) + (q << 5) + (q << 2)));
            number = q;
            outputBuffer[--charPos] = (byte)DigitOnes[r];
            outputBuffer[--charPos] = (byte)DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)number;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            outputBuffer[--charPos] = (byte)DigitOnes[r];
            outputBuffer[--charPos] = (byte)DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));
            outputBuffer[--charPos] = (byte)digits[r];
            i2 = q2;
            if (i2 == 0) break;
        }
        if (sign != 0) {
            outputBuffer[--charPos] = (byte)sign;
        }
        
        return charPos;
    }

    /**
     * Encode the <code>string</code> into UTF8, assume the original string is ISO-8859-1
     * 
     * @param outputBuffer buffer for the encoded number
     * @param string The String to be encoded
     * @return the length of the encoded string
     */
    public static int toUTF8FromLatin1(byte[] outputBuffer, String string) {
        if (string == null) return 0;
        int len = string.length();
        for (int i = 0; i < string.length(); i++) {
            outputBuffer[i] = (byte)string.charAt(i);
        }
        return len;
    }

    public static int toUTF8(byte[] outputBuffer, String string, char[] workingBuffer) {
        if (string == null) {
            return 0;
        } else {
            int strlen = string.length();
            char[] charr = null;
            if (workingBuffer == null || strlen > workingBuffer.length) {
                charr = new char[strlen];
            } else {
                charr = workingBuffer;
            }
            string.getChars(0, strlen, charr, 0);

            int c;
            int count = 0;
            int utflen = 0;
            for (int i = 0; i < strlen; i++) {
                c = charr[i];
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    utflen++;
                } else if (c > 0x07FF) {
                    utflen += 3;
                } else {
                    utflen += 2;
                }
            }

            byte[] bytearr = null;
            if (outputBuffer == null || utflen > outputBuffer.length) {
                bytearr = new byte[utflen];
            } else {
                bytearr = outputBuffer;
            }

            for (int i = 0; i < strlen; i++) {
                c = charr[i];
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    bytearr[count++] = (byte) c;
                } else if (c > 0x07FF) {
                    bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                    bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                    bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
                } else {
                    bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                    bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
                }
            }
            
            return utflen;
        }
    }

    public static byte[] toUTF8(String string) {
        if (string == null) {
            return null;
        }
        int strlen = string.length();
        char[] charr = new char[strlen];
        string.getChars(0, strlen, charr, 0);

        int c;
        int count = 0;
        int utflen = 0;
        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        byte[] bytearr = new byte[utflen];
        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;
            } else if (c > 0x07FF) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            }
        }
        return bytearr;
    }

    public static String getStringFromUTF8(byte[] bytearr, int offset, int utflen) {
        if (utflen < 0) return null;
        else {
            StringBuffer str = new StringBuffer(utflen);
            int c, char2, char3;
            int index = offset;
            int end = offset + utflen;
            while (index < end) {
                c = bytearr[index] & 0xff;
                switch (c >> 4) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        /* 0xxxxxxx */
                        index++;
                        str.append((char) c);
                        break;
                    case 12:
                    case 13:
                        /* 110x xxxx 10xx xxxx */
                        index += 2;
                        char2 = bytearr[index - 1];
                        str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
                        break;
                    case 14:
                        /* 1110 xxxx 10xx xxxx 10xx xxxx */
                        index += 3;
                        char2 = bytearr[index - 2];
                        char3 = bytearr[index - 1];
                        str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
                        break;
                    default :
                        /* 10xx xxxx, 1111 xxxx */
                        throw new RuntimeException("UTF-8 format error");
                }
            }
            // The number of chars produced may be less than utflen
            return new String(str);
        }
    }
}
