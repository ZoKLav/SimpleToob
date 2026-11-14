package com.simpletoob.app;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Independent of account cookies. Provider estimates are not added together. */
public final class RatingsRepository {
    public static final class Source{public final String name;public final long likes,dislikes,at;public final boolean estimated;public final String note;Source(String n,long l,long d,boolean e,String note){name=n;likes=l;dislikes=d;estimated=e;this.note=note;at=System.currentTimeMillis();}}
    public static final class Result{public final List<Source> sources;Result(List<Source> s){sources=s;}public String summary(){long like=-1,dislike=-1;String ds="unavailable";for(Source s:sources){if(like<0&&s.likes>=0)like=s.likes;if(dislike<0&&s.dislikes>=0){dislike=s.dislikes;ds=s.name;}}return "Likes: "+(like<0?"unavailable":String.format(Locale.ROOT,"%,d",like))+"   Dislikes: "+(dislike<0?"unavailable":String.format(Locale.ROOT,"%,d",dislike)+" estimated · "+ds);}
        public String details(){StringBuilder b=new StringBuilder();for(Source s:sources)b.append(s.name).append(s.estimated?" · estimate":" · YouTube-reported").append("\nLikes: ").append(s.likes<0?"unavailable":s.likes).append("  Dislikes: ").append(s.dislikes<0?"unavailable":s.dislikes).append("\n").append(s.note).append("\nFetched: ").append(new Date(s.at)).append("\n\n");return b+"Return YouTube Dislike: https://returnyoutubedislike.com\n\nInvidious instances may reuse RYD. Counts are never summed or presented as independent votes without evidence.";}}
    private final Prefs prefs;private final LinkedHashMap<String,Source> cache=new LinkedHashMap<>();private long retryRydAt;private final java.util.concurrent.ConcurrentHashMap<String,java.util.concurrent.CompletableFuture<Source>> pending=new java.util.concurrent.ConcurrentHashMap<>();
    RatingsRepository(Prefs p){prefs=p;}
    public Result fetch(String id,long youtubeLikes){List<Source> out=new ArrayList<>();out.add(new Source("YouTube",youtubeLikes,-1,false,"Public dislikes are not exposed by YouTube."));if(!VideoIds.valid(id)||!prefs.flag("ratings_enabled",true))return new Result(out);
        if(prefs.flag("ratings_ryd",true))out.add(provider("RYD", "https://returnyoutubedislikeapi.com/votes?videoId="+id,true));
        String other=instance(prefs.text("ratings_instance",""));if(other!=null)out.add(provider("Invidious",other+"/api/v1/videos/"+id,false));return new Result(out);
    }
    public synchronized void clear(){cache.clear();}
    private Source provider(String name,String url,boolean ryd){
        java.util.concurrent.CompletableFuture<Source> mine=new java.util.concurrent.CompletableFuture<>(),other=pending.putIfAbsent(url,mine);
        if(other!=null)try{return other.get(12,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){return new Source(name,-1,-1,true,"Provider is still unavailable.");}
        try{Source source=loadProvider(name,url,ryd);mine.complete(source);return source;}catch(RuntimeException e){mine.completeExceptionally(e);return new Source(name,-1,-1,true,"Provider unavailable.");}finally{pending.remove(url,mine);}
    }
    private Source loadProvider(String name,String url,boolean ryd){
        Source cached; synchronized(this){cached=cache.get(url);if(cached!=null&&System.currentTimeMillis()-cached.at<10*60*1000)return cached;if(ryd&&System.currentTimeMillis()<retryRydAt)return cached!=null?cached:new Source(name,-1,-1,true,"Rate-limited; waiting before retry.");}
        try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","SimpleToob/0.4");try{int code=c.getResponseCode();if(code==429){synchronized(this){retryRydAt=System.currentTimeMillis()+60000;}throw new IOException("Rate limited");}if(code!=200)throw new IOException("HTTP "+code);String raw;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("Response too large");out.write(b,0,n);}raw=out.toString(StandardCharsets.UTF_8.name());}JSONObject j=new JSONObject(raw);long l=count(j,ryd?"likes":"likeCount"),d=count(j,ryd?"dislikes":"dislikeCount");Source s=new Source(name,l,d,true,ryd?"RYD's aggregate estimate; it isn't a live official dislike count.":"Additional provider evidence; may share data with RYD.");synchronized(this){if(cache.size()>=100)cache.remove(cache.keySet().iterator().next());cache.put(url,s);}return s;}finally{c.disconnect();}}
        catch(Exception e){return cached!=null?cached:new Source(name,-1,-1,true,"Provider unavailable; no count was invented.");}
    }
    static long count(JSONObject j,String key){Object n=j.opt(key);if(!(n instanceof Number))return -1;double value=((Number)n).doubleValue();return Double.isFinite(value)&&value>=0&&value<=Long.MAX_VALUE?(long)value:-1;}
    public static String instance(String value){try{URI u=URI.create(value.trim());String h=u.getHost();if(!"https".equals(u.getScheme())||h==null||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||u.getPort()!=-1&&u.getPort()!=443||h.equals("localhost")||!h.contains(".")||h.matches("[0-9.]+")||h.contains(":")||h.endsWith(".local"))return null;return "https://"+h.toLowerCase(Locale.ROOT);}catch(Exception e){return null;}}
}
