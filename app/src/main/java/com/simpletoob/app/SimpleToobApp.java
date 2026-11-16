package com.simpletoob.app;

import android.app.Application;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SimpleToobApp extends Application {
    public ExecutorService network, disk, images, media, account, ratings,importReads,importControl,recommendations;
    public final Object downloadLock=new Object();
    public java.util.concurrent.Future<?> downloadRecovery;
    public ImportManager imports;
    public SessionVault sessions;
    public YoutubeWebClient web;
    public RatingsRepository votes;
    public Prefs prefs;
    public LibraryStore library;
    public YoutubeRepository youtube;
    public ImageLoader thumbnails;
    HomeRecommendations home;
    Avatars avatars;
    public volatile boolean videoActive,audioActive;
    @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(AppLanguage.wrap(base));}
    @Override public void onCreate() {
        super.onCreate();
        importReads=pool(3,12,"simpletoob-import-read");importControl=pool(1,4,"simpletoob-import-control");network=pool(3,48,"simpletoob-network");
        recommendations=pool(2,8,"simpletoob-recommendations");media=pool(2,8,"simpletoob-media");account=pool(1,16,"simpletoob-account");ratings=pool(2,16,"simpletoob-ratings");
        disk=pool(1,256,"simpletoob-disk");
        images=pool(2,32,"simpletoob-images");
        prefs=new Prefs(this); library=new LibraryStore(this,prefs.text("profile","default"));
        sessions=new SessionVault(this,prefs);web=new YoutubeWebClient(sessions,prefs);votes=new RatingsRepository(prefs);
        youtube=new YoutubeRepository(this,prefs);home=new HomeRecommendations(()->new HomeRecommendations.Session(sessions.profile(),sessions.revision(),prefs.number("auth_user",0),prefs.text("language","en"),prefs.text("region","US"),sessions.present()),(profile,revision)->{YoutubeWebClient client=new YoutubeWebClient(sessions,prefs).pin(profile,revision);return client::homeData;});
        thumbnails=new ImageLoader(images);avatars=new Avatars(this);imports=new ImportManager(this);sessions.onChanged(()->{home.clear();imports.sessionChanged();SubscriptionJobService.configure(this);});imports.configure();registerActivityLifecycleCallbacks(new android.app.Application.ActivityLifecycleCallbacks(){int count;long lastForeground;public void onActivityStarted(android.app.Activity a){if(count++==0&&(lastForeground==0||android.os.SystemClock.elapsedRealtime()-lastForeground>30000)){lastForeground=android.os.SystemClock.elapsedRealtime();imports.foreground();}}public void onActivityStopped(android.app.Activity a){count=Math.max(0,count-1);}public void onActivityCreated(android.app.Activity a,android.os.Bundle b){}public void onActivityResumed(android.app.Activity a){}public void onActivityPaused(android.app.Activity a){}public void onActivitySaveInstanceState(android.app.Activity a,android.os.Bundle b){}public void onActivityDestroyed(android.app.Activity a){}});
        // SQLite initialization and metadata expiry never run on the UI thread.
        downloadRecovery=disk.submit(() -> {try(DownloadStore downloads=new DownloadStore(this)){downloads.recover();}library.expireMetadata();});SubscriptionJobService.configure(this);
    }
    private ExecutorService pool(int size,int capacity,String name) {
        return WorkPools.create(name,size,capacity,()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
    }
}
