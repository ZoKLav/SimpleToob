package com.simpletoob.app;

import android.os.*;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Public channel art cache. Only subscription/player rows request missing channel metadata. */
final class Avatars {
    private final SimpleToobApp app;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=WorkPools.create("simpletoob-channel-art",1,32,()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
    private final Map<String,String> known=new ConcurrentHashMap<>();
    private final Set<String> pending=ConcurrentHashMap.newKeySet();
    private final Map<String,List<WeakReference<ImageView>>> waiting=new HashMap<>();
    private final LinkedHashMap<String,JSONObject> saved=new LinkedHashMap<>();
    Avatars(SimpleToobApp app){this.app=app;worker.execute(()->{try{JSONObject data=new JSONObject(app.prefs.text("channel_art_v1","{}"));Iterator<String> keys=data.keys();while(keys.hasNext()){String k=keys.next();JSONObject v=data.optJSONObject(k);if(v!=null&&System.currentTimeMillis()-v.optLong("time")<7L*86400000&&AccountCollections.imageUrl(v.optString("url"))){saved.put(k,v);known.put(k,v.optString("url"));}}}catch(Exception ignored){}});}
    static ImageView view(android.content.Context context,Ui ui){ImageView v=new ImageView(context);v.setScaleType(ImageView.ScaleType.CENTER_CROP);v.setBackground(ui.rounded(ui.surface,100));v.setClipToOutline(true);v.setContentDescription("Channel avatar");return v;}
    void image(String url,ImageView target,int tint){target.setTag(null);target.setImageDrawable(new AppIcons("account",tint));if(AccountCollections.imageUrl(url))app.thumbnails.load(url,target,true);}
    void bind(String channel,String supplied,ImageView target,int tint,boolean resolve){
        String key=ChannelUrls.parse(channel);if(key==null)key="";String url=AccountCollections.imageUrl(supplied)?supplied:known.getOrDefault(key,"");image(url,target,tint);
        if(!url.isEmpty()){if(!key.isEmpty()&&!url.equals(known.put(key,url)))saveLater(key,url);return;}
        if(!resolve||key.isEmpty())return;target.setTag("channel:"+key);
        synchronized(waiting){waiting.computeIfAbsent(key,k->new ArrayList<>()).add(new WeakReference<>(target));}
        final String channelKey=key;if(!pending.add(key))return;
        try{worker.execute(()->{String result="";try{boolean needed=false;synchronized(waiting){List<WeakReference<ImageView>> rows=waiting.get(channelKey);if(rows!=null)for(WeakReference<ImageView> ref:rows){ImageView view=ref.get();if(view!=null&&("channel:"+channelKey).equals(view.getTag())){needed=true;break;}}}result=known.getOrDefault(channelKey,"");if(result.isEmpty()&&needed&&!app.videoActive&&!app.audioActive)result=app.youtube.channelAvatar(channelKey);if(AccountCollections.imageUrl(result)){known.put(channelKey,result);save(channelKey,result);}}catch(Exception ignored){}final String art=result;main.post(()->{pending.remove(channelKey);List<WeakReference<ImageView>> rows;synchronized(waiting){rows=waiting.remove(channelKey);}if(rows!=null)for(WeakReference<ImageView> ref:rows){ImageView view=ref.get();if(view!=null&&("channel:"+channelKey).equals(view.getTag()))image(art,view,tint);}});});}catch(RejectedExecutionException e){pending.remove(key);synchronized(waiting){waiting.remove(key);}}
    }
    private void saveLater(String key,String url){try{worker.execute(()->save(key,url));}catch(RejectedExecutionException ignored){}}
    private void save(String key,String url){try{JSONObject entry=new JSONObject().put("url",url).put("time",System.currentTimeMillis());saved.remove(key);saved.put(key,entry);while(saved.size()>256)saved.remove(saved.keySet().iterator().next());app.prefs.put("channel_art_v1",new JSONObject(saved).toString());}catch(Exception ignored){}}
}
