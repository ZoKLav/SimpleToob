package com.simpletoob.app;
import java.net.URI;
public final class ChannelUrls {
    private ChannelUrls(){}
    public static String parse(String s){
        if(s==null)return null;s=s.trim();if(s.matches("UC[A-Za-z0-9_-]{22}"))return "https://www.youtube.com/channel/"+s;
        try{URI u=URI.create(s);String h=u.getHost(),p=u.getPath();if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||h==null||!(h.equals("youtube.com")||h.equals("www.youtube.com")||h.equals("m.youtube.com")))return null;
            if(p.matches("/channel/UC[A-Za-z0-9_-]{22}/?"))return "https://www.youtube.com"+p.replaceAll("/$","");
            if(p.matches("/@[\\p{L}\\p{N}_.%~-]{1,100}/?"))return "https://www.youtube.com"+p.replaceAll("/$","");
        }catch(Exception ignored){}return null;
    }
}
