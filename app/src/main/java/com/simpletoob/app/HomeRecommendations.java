package com.simpletoob.app;

import java.util.*;
import java.util.function.LongSupplier;
import org.json.JSONObject;

/** Read-only account Home snapshots. In-memory only, scoped to profile + credential revision + content locale. */
final class HomeRecommendations {
    static final class FeedException extends Exception {FeedException(String message){super(message);}}
    static final class Session {final String profile,revision,language,region;final int account;final boolean signedIn;Session(String profile,String revision,int account,String language,String region,boolean signedIn){this.profile=profile;this.revision=revision;this.account=account;this.language=language;this.region=region;this.signedIn=signedIn;}String key(){return HomePolicy.scope(profile,revision,account,language,region)+"\n"+signedIn;}}
    interface State {Session current();}
    interface Client {JSONObject page(String continuation)throws Exception;}
    interface Factory {Client create(String profile,String revision);}

    static final class Page {final List<Video> videos;final String next,notice;Page(List<Video> videos,String next,String notice){this.videos=videos;this.next=next;this.notice=notice;}}
    private static final class Cached {final Page page;final long at;Cached(Page page,long at){this.page=page;this.at=at;}}
    private static final class Cursor {final String scope,token;final Client client;final Set<String> seen;Cursor(String scope,String token,Client client,Set<String> seen){this.scope=scope;this.token=token;this.client=client;this.seen=seen;}}
    private final State state;private final Factory factory;private final LongSupplier clock;private long epoch;
    private final LinkedHashMap<String,Cached> first=new LinkedHashMap<>();private final LinkedHashMap<String,Cursor> cursors=new LinkedHashMap<>();
    HomeRecommendations(State state,Factory factory){this(state,factory,System::currentTimeMillis);}
    HomeRecommendations(State state,Factory factory,LongSupplier clock){this.state=state;this.factory=factory;this.clock=clock;}
    boolean signedIn(){return state.current().signedIn;}
    String scope(){return state.current().key();}
    synchronized void clear(){epoch++;first.clear();cursors.clear();}
    private void check(String scope,long expected)throws FeedException{if(!state.current().signedIn||!scope.equals(scope()))throw new FeedException("The account changed. Reload For you.");synchronized(this){if(epoch!=expected)throw new FeedException("Recommendations changed. Refresh For you.");}}
    Page load(String page,boolean refresh)throws Exception {
        Session selected=state.current();if(!selected.signedIn)throw new FeedException("Sign in to see your YouTube recommendations.");String scope=selected.key(),profile=selected.profile,revision=selected.revision;long requestEpoch;Cursor cursor=null;
        synchronized(this){if(refresh&&page.isEmpty())clear();requestEpoch=epoch;if(page.isEmpty()){Cached cached=first.get(scope);if(cached!=null&&clock.getAsLong()-cached.at<120000&&(cached.page.next.isEmpty()||cursors.containsKey(cached.page.next)))return cached.page;}else{cursor=cursors.get(page);if(cursor==null||!scope.equals(cursor.scope))throw new FeedException("This recommendations page expired. Refresh For you.");}}
        Client client=cursor==null?factory.create(profile,revision):cursor.client;String token=cursor==null?"":cursor.token;Set<String> seen=cursor==null?new HashSet<>():new HashSet<>(cursor.seen);if(!token.isEmpty())seen.add(token);
        HomeFeedParser.Result result;
        try{check(scope,requestEpoch);result=HomeFeedParser.parse(client.page(token));check(scope,requestEpoch);}catch(Exception error){check(scope,requestEpoch);throw error;}
        if(result.signedOut)throw new FeedException("Your YouTube session needs verification. Open Account or sign in again.");
        if(!result.recognized)throw new FeedException("YouTube returned an unsupported recommendations layout. Retry or use Popular now.");String next=result.next,notice=result.notice;
        if(!next.isEmpty()&&(seen.contains(next)||seen.size()>=100)){next="";notice="YouTube repeated or limited this recommendations feed. Refresh for more.";}
        synchronized(this){check(scope,requestEpoch);String continuation="";if(!next.isEmpty()){if(cursors.size()>=64)cursors.remove(cursors.keySet().iterator().next());continuation=UUID.randomUUID().toString();cursors.put(continuation,new Cursor(scope,next,client,seen));}Page loaded=new Page(result.videos,continuation,notice);if(page.isEmpty()){if(first.size()>=4)first.remove(first.keySet().iterator().next());first.put(scope,new Cached(loaded,clock.getAsLong()));}return loaded;}
    }
}
