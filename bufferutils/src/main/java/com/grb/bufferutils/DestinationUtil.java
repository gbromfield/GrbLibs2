package com.grb.bufferutils;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class DestinationUtil {
    static private final String ERROR_TOPIC_IS_NULL = "Topic cannot be null";
    static private final String ERROR_TOPIC_TOO_SHORT = "Topic must have a minimum length of ";
    static private final String ERROR_TOPIC_TOO_LONG = "Topic must have a maximum length of ";
    static private final String ERROR_TOPIC_HAS_ILLEGAL_GREATER_THAN = "Topic contains illegally used character [>]";
    static private final String ERROR_TOPIC_HAS_ILLEGAL_STAR = "Topic contains illegally used character [*]";
    static private final String ERROR_TOPIC_HAS_EMPTY_LEVEL = "Topic contains empty level";
    static private final String ERROR_TOPIC_HAS_ILLEGAL_CHAR = "Topic contains illegal character [";
    static private final String ERROR_TOPIC_HAS_ILLEGAL_CONTROL_CHAR = "Topic contains illegal control character [";
    static private final String ERROR_TOPIC_HAS_ILLEGAL_WHITESPACE = "Topic contains illegal whitespace";
    static public final String ERROR_TOPIC_IS_WILD = "Cannot publish to wildcard topic";

    static private final String ERROR_QUEUE_HAS_ILLEGAL_CHAR ="Queue name \"%s\" contains illegal character [%s]";
    static private final String ERROR_QUEUE_HAS_ILLEGAL_CONTROL_CHAR = "Queue name \"%s\" contains illegal control character [%d]";

    static public final char DestinationDelimiter = '/';
    static public final char WildCardChar = '>';
    static public final String WildCardStr = new String(new char[]{WildCardChar});
    static public final byte WildCardByte = WildCardChar;

    static public final int MIN_TOPIC_LENGTH = 1;
    static public final int MAX_TOPIC_LENGTH = 250;

    static public final int MIN_QUEUE_LENGTH = 1;
    static public final int MAX_QUEUE_LENGTH = 200;
    static public final int MAX_QUEUE_LENGTH_TEMPQUEUE = 250;
    
    
	static private final String ERROR_DTE_NAME_TOO_LONG = "Durable Topic Endpoint name \"%s\" must have a maximum length of %s";
	static private final String ERROR_NAME_IS_NULL = "Name cannot be null";
	static private final String ERROR_DTE_HAS_ILLEGAL_CHAR = "Durable Topic Endpoint name \"%s\" contains illegal character, allowed characters are [a-zA-Z0-9_]";
    static private final String ERROR_QUEUE_HAS_EMPTY_LEVEL = "Queue name \"%s\" contains empty level";
    static private final String ERROR_QUEUE_NAME_TOO_LONG = "Queue name \"%s\" must have a maximum length of %s";

    // It's still 32, but we want the validation to be permissive (250 bytes)
	// static public final int MAX_DTE_LENGTH = 32;
    static public final int MAX_DTE_LENGTH = 250;

	static public final String DTE_PROHIB_PATTERN = ".*[^a-zA-Z0-9_].*";
	
    static public boolean isWildCardedCrb(String topic) {
        if (isGlobalWildcard(topic)) {
            return true;
        }
        int l = topic.length();
        return ((l > 2) && (topic.charAt(l - 1) == '>') && (topic.charAt(l - 2) == DestinationDelimiter));
    }

    static public boolean isWildCardedCrb(byte[] topic) {
        if (isGlobalWildcard(topic)) {
            return true;
        }
        int l = topic.length;
        return ((l > 2) && (topic[l - 1] == '>') && (topic[l - 2] == DestinationDelimiter));        
    }

    static public boolean isWildCardedTrb(String topic) {
		if (isGlobalWildcard(topic)) {
			return true;
		}
		int l = topic.length();
		if (l >= 2 
			&& topic.charAt(l - 2) == DestinationDelimiter
			&& topic.charAt(l - 1) == WildCardChar) {
			return true;
		}
		
		for (int i = 0; i < l; i++) {
			if (topic.charAt(i) == '*') {
				return true;
			}
		}
		return false;
	}
    
    static public boolean isGlobalWildcard(String topic) {
        return topic.equals(WildCardStr);        
    }

    static public boolean isGlobalWildcard(byte[] topic) {
        return ((topic.length == 1) && (topic[0] == WildCardByte));        
    }
    
    static public void isValidTopicTrb(String topic, boolean isPub) throws IllegalArgumentException {
		/*
		 * TRB topics can contain any utf-8 character and must be <= 250 bytes
		 * in length.
		 * 
		 * '*', if present in a level, must be the last character in that level.
		 */

		if (topic == null) {
			throw new IllegalArgumentException(ERROR_TOPIC_IS_NULL);
		}
		byte[] topic_data_utf8 = null;
		try {
			topic_data_utf8 = topic.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Never happens (UTF-8) is guaranteed
			topic_data_utf8 = topic.getBytes();
		}
		
		int length = topic_data_utf8.length;
		if (length < MIN_TOPIC_LENGTH) {
			throw new IllegalArgumentException(ERROR_TOPIC_TOO_SHORT + MIN_TOPIC_LENGTH);
		}
		if (length > MAX_TOPIC_LENGTH) {
			throw new IllegalArgumentException(ERROR_TOPIC_TOO_LONG + MAX_TOPIC_LENGTH  + " - " + topic);
		}

		final int numchars = topic.length();
		for (int i = 0; i < numchars; i++) {
			char c = topic.charAt(i);
			if (c == '*' && !isPub) {
				// must not be to the left of something that's not a level
				// delimiter

				// To the right
				if (i < (numchars - 1)) {
					char right = topic.charAt(i + 1);
					if (right != DestinationDelimiter) {
						throw new IllegalArgumentException(ERROR_TOPIC_HAS_ILLEGAL_STAR + " - " + topic);
					}
				}
			} else if (c == DestinationDelimiter) {
                if ((i == 0) || (i == (length-1))) {
                    throw new IllegalArgumentException(ERROR_TOPIC_HAS_EMPTY_LEVEL + " - " + topic);                
                }
                if (topic.charAt(i-1) == DestinationDelimiter) {
                    throw new IllegalArgumentException(ERROR_TOPIC_HAS_EMPTY_LEVEL + " - " + topic);                
                }
			}
		}

	}
    
    /*
     * CRB
     * 
     * Do not allow wildcard on publish. Do not allow '*' at all.
     */
    static public void isValidTopic(String topic, boolean isPub) throws IllegalArgumentException {
        if (topic == null) {
            throw new IllegalArgumentException(ERROR_TOPIC_IS_NULL);
        }
        int length = topic.length();
        if (length < MIN_TOPIC_LENGTH) {
            throw new IllegalArgumentException(ERROR_TOPIC_TOO_SHORT + MIN_TOPIC_LENGTH);
        }
        if (length > MAX_TOPIC_LENGTH) {
            throw new IllegalArgumentException(ERROR_TOPIC_TOO_LONG + MAX_TOPIC_LENGTH);
        }
        char c;
        for (int i = 0; i < length; i++) {
            c = topic.charAt(i);
            if (c == '>') {
            	if (isPub) {
                    throw new IllegalArgumentException(ERROR_TOPIC_HAS_ILLEGAL_GREATER_THAN);                
            	} else {
                    if (length != 1) {
                        if ((i != (length-1)) || (topic.charAt(i-1) != DestinationDelimiter)) {
                            throw new IllegalArgumentException(ERROR_TOPIC_HAS_ILLEGAL_GREATER_THAN);                
                        }
                    }
            	}
            } else if (c == DestinationDelimiter) {
                if ((i == 0) || (i == (length-1))) {
                    throw new IllegalArgumentException(ERROR_TOPIC_HAS_EMPTY_LEVEL);                
                }
                if (topic.charAt(i-1) == DestinationDelimiter) {
                    throw new IllegalArgumentException(ERROR_TOPIC_HAS_EMPTY_LEVEL);                
                }
            } else if ((c == '\"') || (c == '&') || (c == '*') || (c == '\'') || (c == '<')) {
                throw new IllegalArgumentException(ERROR_TOPIC_HAS_ILLEGAL_CHAR + c + "]");                
            } else if ((c == ' ') || (c == '\t')) {
                throw new IllegalArgumentException(ERROR_TOPIC_HAS_ILLEGAL_WHITESPACE);                
            } else if (Character.isISOControl(c)) {
                throw new IllegalArgumentException(ERROR_TOPIC_HAS_ILLEGAL_CONTROL_CHAR + (int)c + "]");                
            }
        }
    }
    
    /**
     * Only valid for Topics.
     * @deprecated
     * @param destination
     * @return
     */
    static public String normalizeDestination(String destination) {
        return destination.toLowerCase();
    }
    
	public static void isValidQueuePhysicalName(String name, boolean isDurable) throws IllegalArgumentException {
		/*
		 * Valid queue name is a case-sensitive UTF-8 string, max 250 bytes in
		 * length. Excluded characters: ['*<>&?]
		 */

		if (name == null || name.equals("")) {
			throw new IllegalArgumentException(ERROR_NAME_IS_NULL);
		}

		char c;
		final int length = name.length();
		for (int i = 0; i < length; i++) {
			c = name.charAt(i);
			if (c == '\'' || c == '*' || c == '<' || c == '>' || c == '&' || c == ';' || c == '?') {
				throw new IllegalArgumentException(String.format(ERROR_QUEUE_HAS_ILLEGAL_CHAR, name, c));
            } else if (c == 0) {
				throw new IllegalArgumentException(String.format(ERROR_QUEUE_HAS_ILLEGAL_CONTROL_CHAR, name, (int)c));
			} else if (c == DestinationUtil.DestinationDelimiter) {
				if ((i == 0) || (i == (length - 1))) {
					throw new IllegalArgumentException(String.format(ERROR_QUEUE_HAS_EMPTY_LEVEL, name));
				}
				if (name.charAt(i - 1) == DestinationUtil.DestinationDelimiter) {
					throw new IllegalArgumentException(String.format(ERROR_QUEUE_HAS_EMPTY_LEVEL, name));
				}
			}
		}

		final int max_queue_len = isDurable ? MAX_QUEUE_LENGTH : MAX_QUEUE_LENGTH_TEMPQUEUE;
		try {
			byte[] nameBytes = name.getBytes("UTF-8");
			if (nameBytes.length > max_queue_len) {
				throw new IllegalArgumentException(String
					.format(ERROR_QUEUE_NAME_TOO_LONG, name, max_queue_len));
			}
		} catch (UnsupportedEncodingException ex) {
		}
	}

	public static void isValidDTEPhysicalName(String name) throws IllegalArgumentException {
		/*
		 * Valid DTE name is an ASCII string, max 32 bytes in length. Allowed
		 * characters: [a-zA-Z0-9_]
		 */

		if (name == null || name.equals("")) {
			throw new IllegalArgumentException(ERROR_NAME_IS_NULL);
		}

		if (name.matches(DTE_PROHIB_PATTERN)) {
			throw new IllegalArgumentException(String.format(ERROR_DTE_HAS_ILLEGAL_CHAR, name));
		}

		try {
			byte[] nameBytes = name.getBytes("US-ASCII");
			if (nameBytes.length > MAX_DTE_LENGTH) {
				throw new IllegalArgumentException(String.format(ERROR_DTE_NAME_TOO_LONG,
					name, MAX_DTE_LENGTH));
			}
		} catch (UnsupportedEncodingException ex) {
		}
	} 
	
	public static boolean haslineBreak(String name) throws IllegalArgumentException {
		if (name == null || name.equals("")) {
			throw new IllegalArgumentException(ERROR_NAME_IS_NULL);
		}
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) == '\r' || name.charAt(i) == '\n') {
				return true;
			}
		}
		return false;
	}
	
	// TRB topic creation
	/**
	 * Creates a TRB topic for a non-durable queue.
	 */
	public static String createNonDurQueueTrbTopic(String vridName, String name) {
	    if (name == null) {
	        return String.format("#P2P/QTMP/%s/%s", vridName, UUID.randomUUID().toString());
	    } else {
	        return String.format("#P2P/QTMP/%s/%s", vridName, name);
	    }
	}

	/**
	 * Creates a TRB topic for a durable queue.
	 */
	public static String createDurQueueTrbTopic(String qName, String hostId) {
	    if (hostId == null) {
	        return String.format("#P2P/QUE/%s", qName);
	    } else {
	        return String.format("#P2P/QUE/%s/%s", hostId, qName);
	    }
	}
	
	/**
	 * Creates a TRB topic for a temporary topic.
	 */
	public static String createNonDurTopicTrbTopic(String vridName, String name) {
	    if (name == null) {
	        return String.format("#P2P/TTMP/%s/%s", vridName, UUID.randomUUID().toString());
	    } else {
	        return String.format("#P2P/TTMP/%s/%s", vridName, name);
	    }
	}
}
