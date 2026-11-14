package com.simpletoob.app;

import org.schabi.newpipe.extractor.downloader.*;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Public extraction requests only. No Google account cookies or password handling. */
public final class WebDownloader extends Downloader {
    public static final String USER_AGENT="Mozilla/5.0 (X11; Linux x86_64; rv:153.0) Gecko/20100101 Firefox/153.0";
    public static boolean mediaUrl(String value) {
        try { URI u=URI.create(value);String h=u.getHost();return "https".equals(u.getScheme()) && u.getUserInfo()==null && h!=null && (h.equals("googlevideo.com")||h.endsWith(".googlevideo.com")); }
        catch(Exception e){return false;}
    }
    public static String mediaUserAgent(String url) {
        return org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isVisionOsStreamingUrl(url)
            ?org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getVisionOsUserAgent(null):USER_AGENT;
    }
    private static boolean allowed(String value) {
        try {URI u=URI.create(value);String h=u.getHost();if(!"https".equals(u.getScheme())||h==null||u.getUserInfo()!=null)return false;
            for(String d:new String[]{"youtube.com","youtu.be","google.com","googlevideo.com","ytimg.com","googleapis.com","ggpht.com","googleusercontent.com"})if(h.equals(d)||h.endsWith("."+d))return true;
        }catch(Exception ignored){}return false;
    }
    @Override public Response execute(Request request) throws IOException,ReCaptchaException {
        String url=request.url(),method=request.httpMethod();byte[] body=request.dataToSend();
        for(int redirect=0;redirect<6;redirect++) {
            if(!allowed(url))throw new IOException("Unsupported extraction host");
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
            c.setRequestProperty("User-Agent",USER_AGENT);
            for(Map.Entry<String,List<String>> h:request.headers().entrySet()){boolean first=true;for(String value:h.getValue()){if(first)c.setRequestProperty(h.getKey(),value);else c.addRequestProperty(h.getKey(),value);first=false;}}
            try {
                if(body!=null && !method.equals("GET") && !method.equals("HEAD")) {c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body);}}
                int code=c.getResponseCode();
                if(code>=300&&code<400) {
                    String location=c.getHeaderField("Location");if(location==null)throw new IOException("Redirect without destination");
                    url=new URL(new URL(url),location).toString();
                    if(code==303||((code==301||code==302)&&method.equals("POST"))){method="GET";body=null;}
                    continue;
                }
                InputStream raw=code<400?c.getInputStream():c.getErrorStream();String text="";
                if(raw!=null)try(InputStream in="gzip".equalsIgnoreCase(c.getContentEncoding())?new GZIPInputStream(raw):raw;ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] b=new byte[8192];int n;
                    while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>16*1024*1024)throw new IOException("Response too large");out.write(b,0,n);}
                    text=out.toString(StandardCharsets.UTF_8.name());
                }
                return new Response(code,c.getResponseMessage(),c.getHeaderFields(),text,url);
            }finally{c.disconnect();}
        }
        throw new IOException("Too many redirects");
    }
}
