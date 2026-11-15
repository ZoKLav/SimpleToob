package com.simpletoob.app;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.datasource.*;
import androidx.media3.exoplayer.DefaultLoadControl;
import java.io.*;
import java.util.*;

/** Bounded progressive CDN requests; HLS and other transports retain the standard Media3 path. */
final class MediaNetwork {
    private MediaNetwork(){}
    static DefaultLoadControl buffers(Prefs prefs,boolean audio){int seconds=Math.max(15,Math.min(120,prefs.number("buffer_seconds",60)));return new DefaultLoadControl.Builder().setBufferDurationsMs(15000,seconds*1000,1000,3000).setTargetBufferBytes((audio?4:32)*1024*1024).setPrioritizeTimeOverSizeThresholds(false).build();}
    static DataSource.Factory factory(Prefs prefs){return ()->new Segments(prefs.flag("chunked_streaming",true));}
    private static final class Segments implements DataSource {
        private final boolean enabled;private final List<TransferListener> listeners=new ArrayList<>();
        private DataSource source;private DataSpec original;private long cursor,end,chunkEnd;private boolean chunked;
        private Map<String,List<String>> headers=Collections.emptyMap();private Uri uri;
        Segments(boolean enabled){this.enabled=enabled;}
        @Override public void addTransferListener(TransferListener l){listeners.add(l);if(source!=null)source.addTransferListener(l);}
        @Override public long open(DataSpec spec)throws IOException{original=spec;cursor=spec.position;end=spec.length==C.LENGTH_UNSET?Long.MAX_VALUE:cursor+spec.length;String host=spec.uri.getHost();chunked=enabled&&host!=null&&(host.equals("googlevideo.com")||host.endsWith(".googlevideo.com"))&&spec.httpMethod==DataSpec.HTTP_METHOD_GET;return openPart();}
        private long openPart()throws IOException{
            Map<String,String> request=new HashMap<>(original.httpRequestHeaders);String url=original.uri.toString();request.put("User-Agent",WebDownloader.mediaUserAgent(url));request.put("Referer","https://www.youtube.com/");request.put("Origin","https://www.youtube.com");request.put("Accept-Encoding","identity");
            DefaultHttpDataSource http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setAllowCrossProtocolRedirects(false).createDataSource();source=http;for(TransferListener l:listeners)source.addTransferListener(l);
            long amount=end==Long.MAX_VALUE?C.LENGTH_UNSET:end-cursor;if(chunked)amount=amount==C.LENGTH_UNSET?1024*1024:Math.min(amount,1024*1024);
            DataSpec spec=original.buildUpon().setPosition(cursor).setLength(amount).setHttpRequestHeaders(request).build();
            long available;try{available=source.open(spec);headers=source.getResponseHeaders();uri=source.getUri();
                if(chunked&&http.getResponseCode()==206){long[] range=ByteRanges.parse(header(headers,"Content-Range"));if(range==null||range[0]!=cursor||range[1]!=cursor+amount-1&&range[1]!=range[2]-1)throw new IOException("Invalid media byte range");end=Math.min(end,range[2]);chunkEnd=Math.min(end,range[1]+1);}
                else if(chunked){source.close();source=null;chunked=false;return openPart();}
                else {chunkEnd=end;if(available!=C.LENGTH_UNSET){end=Math.min(end,cursor+available);chunkEnd=end;}}
            }catch(IOException e){try{source.close();}catch(Exception ignored){}source=null;throw e;}
            return end==Long.MAX_VALUE?C.LENGTH_UNSET:end-cursor;
        }
        @Override public int read(byte[] buffer,int offset,int length)throws IOException{
            if(length==0)return 0;if(cursor>=end)return C.RESULT_END_OF_INPUT;
            if(chunked&&cursor>=chunkEnd){source.close();source=null;openPart();}
            int want=chunked?(int)Math.min(length,chunkEnd-cursor):length;int read=source.read(buffer,offset,want);
            if(read>0)cursor+=read;else if(read==C.RESULT_END_OF_INPUT&&chunked&&cursor<chunkEnd)throw new EOFException("Media range ended early");return read;
        }
        @Override public Uri getUri(){return uri;}
        @Override public Map<String,List<String>> getResponseHeaders(){return headers;}
        @Override public void close()throws IOException{if(source!=null){try{source.close();}finally{source=null;}}}
    }
    private static String header(Map<String,List<String>> rows,String name){for(Map.Entry<String,List<String>> row:rows.entrySet())if(name.equalsIgnoreCase(row.getKey())&&!row.getValue().isEmpty())return row.getValue().get(0);return "";}
}
