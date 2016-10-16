package com.grb.bufferutils;

public class Reflection {

    static public String getMethodName() {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement[] elements = t.getStackTrace();
        if (elements.length > 1) {
            return elements[1].getMethodName();
        }
        return "";
    }
}
