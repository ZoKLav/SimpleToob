package com.simpletoob.app;

import java.util.regex.*;
/** Strict HTTP Content-Range parser shared by streaming and file transfers. */
final class ByteRanges {
    private static final Pattern RANGE=Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)");
    static long[] parse(String value){if(value==null)return null;Matcher m=RANGE.matcher(value);if(!m.matches())return null;try{long a=Long.parseLong(m.group(1)),b=Long.parseLong(m.group(2)),n=Long.parseLong(m.group(3));return a<=b&&b<n?new long[]{a,b,n}:null;}catch(NumberFormatException e){return null;}}
}
