package com.grb.tl1;

/**
 * Created by gbromfie on 11/5/15.
 */
public class TL1MessageMaxSizeExceededException extends Exception {
	private static final long serialVersionUID = 1L;

	public TL1MessageMaxSizeExceededException(String message) {
        super(message);
    }
}
