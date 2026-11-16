package com.simpletoob.app;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

/** Opt-in public subscription polling, independent of sign-in and account imports. */
public final class SubscriptionJobService extends JobService {
    private static final int JOB=4110;private static final String CHANNEL="subscription_updates";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=WorkPools.create("simpletoob-subscriptions",1,1,()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
    private Future<?> request;private volatile JobParameters active;private volatile boolean destroyed;
    static String key(SimpleToobApp app){return "subscription_notifications."+app.sessions.profile();}
    static void configure(SimpleToobApp app){
        JobScheduler scheduler=app.getSystemService(JobScheduler.class);boolean allowed=Build.VERSION.SDK_INT<33||app.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED;
        if(!app.prefs.flag(key(app),false)||!allowed){scheduler.cancel(JOB);return;}
        String profile=app.sessions.profile(),revision=app.sessions.revision(),owner=app.imports.collectionOwner();if(owner==null)owner="*";
        JobInfo old=scheduler.getPendingJob(JOB);if(old!=null&&profile.equals(old.getExtras().getString("profile"))&&revision.equals(old.getExtras().getString("revision"))&&owner.equals(old.getExtras().getString("owner")))return;
        PersistableBundle extras=new PersistableBundle();extras.putString("profile",profile);extras.putString("revision",revision);extras.putString("owner",owner);
        scheduler.schedule(new JobInfo.Builder(JOB,new ComponentName(app,SubscriptionJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(60*60*1000L).setExtras(extras).build());
    }
    private boolean valid(SimpleToobApp app,JobParameters job){String owner=app.imports.collectionOwner();return !destroyed&&active==job&&!Thread.currentThread().isInterrupted()&&app.prefs.flag(key(app),false)&&app.sessions.profile().equals(job.getExtras().getString("profile"))&&app.sessions.revision().equals(job.getExtras().getString("revision"))&&Objects.equals(owner==null?"*":owner,job.getExtras().getString("owner"));}
    @Override public boolean onStartJob(JobParameters job){SimpleToobApp app=(SimpleToobApp)getApplication();active=job;if(!valid(app,job)){active=null;return false;}if(app.videoActive||app.audioActive){active=null;return false;}
        try{request=worker.submit(()->{try{poll(app,job);}catch(Exception ignored){}finally{if(!destroyed)main.post(()->{if(active==job){active=null;jobFinished(job,false);}});}});return true;}catch(RejectedExecutionException e){active=null;return false;}
    }
    private void poll(SimpleToobApp app,JobParameters job)throws Exception{
        String profile=job.getExtras().getString("profile"),owner=app.imports.collectionOwner(),scope=profile+":"+(owner==null?"*":owner);
        LibraryStore library=app.library;List<LibraryStore.Subscription> channels=library.subscriptions(owner);if(channels.isEmpty())return;
        android.content.SharedPreferences snapshots=getSharedPreferences("subscription_updates",MODE_PRIVATE);int offset=snapshots.getInt(scope+":cursor",0)%channels.size(),processed=0;long deadline=SystemClock.elapsedRealtime()+90000;List<Video> fresh=new ArrayList<>();
        for(int n=0;n<Math.min(10,channels.size())&&SystemClock.elapsedRealtime()<deadline;n++){
            if(!valid(app,job))return;LibraryStore.Subscription channel=channels.get((offset+n)%channels.size());processed++;
            try{YoutubeRepository.Page page=app.youtube.search("","","","",channel.url);if(!valid(app,job))return;if(page.videos.isEmpty())continue;String key=scope+":"+channel.url;SubscriptionUpdates.Update update=SubscriptionUpdates.compare(snapshots.getString(key,null),page.videos);
                if(!snapshots.edit().putString(key,update.snapshot).commit())continue;fresh.addAll(update.fresh);
            }catch(Exception e){if(Thread.currentThread().isInterrupted())return;}
        }
        if(!valid(app,job))return;snapshots.edit().putInt(scope+":cursor",(offset+processed)%channels.size()).apply();
        // Retain only subscribed channels in the currently checked scope.
        Set<String> keys=new HashSet<>();for(LibraryStore.Subscription c:channels)keys.add(scope+":"+c.url);android.content.SharedPreferences.Editor prune=snapshots.edit();for(String k:snapshots.getAll().keySet())if(k.startsWith(scope+":")&&!k.equals(scope+":cursor")&&!keys.contains(k))prune.remove(k);prune.apply();
        if(fresh.isEmpty())return;NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel(CHANNEL,I18n.text(this,"Subscription notifications"),NotificationManager.IMPORTANCE_DEFAULT));
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        PendingIntent open=PendingIntent.getActivity(this,4110,new Intent(this,MainActivity.class).putExtra("open_screen","Subs"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(4110,new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle(I18n.text(this,"New subscription videos")).setContentText(fresh.get(0).title).setNumber(fresh.size()).setContentIntent(open).setAutoCancel(true).build());
    }
    @Override public boolean onStopJob(JobParameters job){if(active==job){active=null;if(request!=null)request.cancel(true);}main.removeCallbacksAndMessages(null);return false;}
    @Override public void onDestroy(){destroyed=true;active=null;if(request!=null)request.cancel(true);worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
