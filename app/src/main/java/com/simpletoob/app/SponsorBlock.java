package com.simpletoob.app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Optional public lookup. No session cookies, submissions, votes or viewed-segment telemetry. */
final class SponsorBlock implements AutoCloseable {
    static final class Segment {final long start,end;Segment(long start,long end){this.start=start;this.end=end;}}
    private volatile HttpURLConnection active;private volatile boolean closed;
    static String hash(String id)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    List<Segment> fetch(String id)throws Exception{
        if(!VideoIds.valid(id)||closed)return Collections.emptyList();String hash=hash(id);
        HttpURLConnection c=(HttpURLConnection)new URL("https://sponsor.ajay.app/api/skipSegments/"+hash.substring(0,4)+"?categories=%5B%22sponsor%22%5D&actionTypes=%5B%22skip%22%5D").openConnection();active=c;
        try{if(closed)throw new InterruptedIOException();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("Accept","application/json");if(c.getResponseCode()==404)return Collections.emptyList();if(c.getResponseCode()!=200)throw new IOException("SponsorBlock unavailable");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>1024*1024)throw new IOException("SponsorBlock response too large");out.write(b,0,n);}return parse(out.toString("UTF-8"),hash);}
        }finally{c.disconnect();active=null;}
    }
    static List<Segment> parse(String json,String hash)throws JSONException{
        List<Segment> result=new ArrayList<>();JSONArray rows=new JSONArray(json);for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null||!hash.equals(row.optString("hash")))continue;JSONArray segments=row.optJSONArray("segments");if(segments==null)continue;for(int j=0;j<segments.length()&&result.size()<1000;j++){JSONObject s=segments.optJSONObject(j);if(s==null||!"sponsor".equals(s.optString("category"))||!"skip".equals(s.optString("actionType")))continue;JSONArray times=s.optJSONArray("segment");if(times==null||times.length()!=2)continue;double a=times.optDouble(0),b=times.optDouble(1);if(!Double.isFinite(a)||!Double.isFinite(b)||a<0||b<=a||b>7*24*3600)continue;result.add(new Segment((long)(a*1000),(long)(b*1000)));}}
        result.sort(Comparator.comparingLong(s->s.start));return Collections.unmodifiableList(result);
    }
    static long endAt(List<Segment> segments,long position,long duration){long end=position;for(Segment s:segments){if(s.start>end)break;if(s.end>end&&s.start<=end&&s.end<=duration)end=s.end;}return end;}
    public void close(){closed=true;HttpURLConnection c=active;if(c!=null)c.disconnect();}
}
