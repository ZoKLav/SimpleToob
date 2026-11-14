package com.simpletoob.app;
import java.net.URI;
import java.util.*;
/** In-memory redirect budget. No query strings or page content are retained or logged. */
public final class LoginNavigation {
    private static final long WINDOW_MS=15000;
    private static final class Visit {final String route;final long at;Visit(String r,long t){route=r;at=t;}}
    private final ArrayDeque<Visit> visits=new ArrayDeque<>();private boolean stopped;
    public boolean start(String url,long now){
        if(stopped)return false;
        String route;try{URI u=URI.create(url);route=u.getHost()+Objects.toString(u.getPath(),"/");}catch(Exception e){route="invalid";}
        while(!visits.isEmpty()&&now-visits.peekFirst().at>=WINDOW_MS)visits.removeFirst();
        int repeats=0;for(Visit v:visits)if(v.route.equals(route))repeats++;
        if(repeats>=5||visits.size()>=16){stopped=true;return false;}
        visits.addLast(new Visit(route,now));return true;
    }
    public boolean stopped(){return stopped;}
    public void reset(){visits.clear();stopped=false;}
}
