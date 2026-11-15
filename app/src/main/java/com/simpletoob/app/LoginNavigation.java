package com.simpletoob.app;
import java.net.URI;
import java.util.*;
/** In-memory redirect budget. No query strings or page content are retained or logged. */
public final class LoginNavigation {
    private static final long WINDOW_MS=15000;
    private static final class Visit {final String route;final long at;Visit(String r,long t){route=r;at=t;}}
    private final ArrayDeque<Visit> visits=new ArrayDeque<>();private final Map<String,Integer> chain=new HashMap<>();private int chainLength;private boolean stopped;
    public boolean start(String url,long now){
        if(stopped)return false;
        String route;try{URI u=URI.create(url);route=u.getHost()+Objects.toString(u.getPath(),"/");}catch(Exception e){route="invalid";}
        while(!visits.isEmpty()&&now-visits.peekFirst().at>=WINDOW_MS)visits.removeFirst();
        int repeats=0;for(Visit v:visits)if(v.route.equals(route))repeats++;
        // A slow redirect loop can evade a sliding window forever. Only a real user action resets this chain.
        int total=chain.getOrDefault(route,0);if(repeats>=5||visits.size()>=16||total>=8||chainLength>=48){stopped=true;return false;}
        chain.put(route,total+1);chainLength++;
        visits.addLast(new Visit(route,now));return true;
    }
    public boolean stopped(){return stopped;}
    public void reset(){visits.clear();chain.clear();chainLength=0;stopped=false;}
    public static boolean landing(String url){if(!WebJson.youtube(url))return false;try{String p=Objects.toString(URI.create(url).getPath(),"/").toLowerCase(Locale.ROOT);return !p.startsWith("/signin")&&!p.startsWith("/logout")&&!p.startsWith("/channel_switcher")&&!p.startsWith("/select_site")&&!p.startsWith("/oops");}catch(Exception ignored){return false;}}
}
