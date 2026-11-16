package com.simpletoob.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/** Serialized persistent downloads. Only an explicit foreground action starts or resumes work. */
public final class DownloadService extends Service {
    public static volatile String state="";
    private static final String CHANNEL="downloads";
    private final ExecutorService worker=WorkPools.create("simpletoob-download",1,16,()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean recovered;private volatile boolean cancelled,destroyed;private final java.util.concurrent.atomic.AtomicInteger pauseRequest=new java.util.concurrent.atomic.AtomicInteger();private final ConcurrentHashMap<String,Integer> held=new ConcurrentHashMap<>();private volatile RangeTransfer transfer;private volatile DownloadStore.Job activeJob;
    private final java.util.concurrent.atomic.AtomicInteger pending=new java.util.concurrent.atomic.AtomicInteger();private long notified;private NotificationManager notifications;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(AppLanguage.wrap(base));}
    @Override public void onCreate(){super.onCreate();notifications=getSystemService(NotificationManager.class);notifications.createNotificationChannel(new NotificationChannel(CHANNEL,I18n.text(this,"Video downloads"),NotificationManager.IMPORTANCE_LOW));}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf(startId);return START_NOT_STICKY;}
        String action=intent.getAction(),id=intent.getStringExtra("job");DownloadStore.Job running=activeJob;String requestProfile=intent.getStringExtra("profile");final String profile=requestProfile==null?((SimpleToobApp)getApplication()).sessions.profile():requestProfile;
        if("pause".equals(action)||"cancel".equals(action)||"remove".equals(action)){if(id==null)pauseRequest.set(startId);else held.put(id,startId);}
        if(("pause".equals(action)||"cancel".equals(action)||"remove".equals(action))&&(id==null||running!=null&&id.equals(running.id))){cancelled=true;RangeTransfer t=transfer;if(t!=null)t.close();}
        startForeground(20,notification("SimpleToob",I18n.text(this,"Preparing downloads…"),true));
        pending.incrementAndGet();try{worker.execute(()->{synchronized(((SimpleToobApp)getApplication()).downloadLock){try(DownloadStore store=new DownloadStore(this)){
            if(destroyed||Thread.currentThread().isInterrupted())return;((SimpleToobApp)getApplication()).downloadRecovery.get();if(!recovered){store.recover();recovered=true;}
            if(!"pause".equals(action)&&!"cancel".equals(action)&&!"remove".equals(action)&&!"resume".equals(action)&&!"enqueue_playlist".equals(action)&&intent.hasExtra("video_id")){
                if(!profile.equals(((SimpleToobApp)getApplication()).sessions.profile()))throw new IOException("Profile changed");String video=intent.getStringExtra("video_id");store.enqueue(profile,Collections.singletonList(new Video(video,intent.getStringExtra("title"),"","","","","",0)),intent.getStringExtra("output"),"",value(intent,"choice"),value(intent,"audio_track"));
            }
            if(action!=null)for(DownloadStore.Job j:store.list(profile))if(id==null||j.id.equals(id)){
                if("resume".equals(action)&&("paused".equals(j.state)||"failed".equals(j.state)))store.state(j,"queued","");
                if("pause".equals(action)&&("queued".equals(j.state)||"running".equals(j.state)))store.state(j,"paused","");
                if("cancel".equals(action)||"remove".equals(action)){if(!"complete".equals(j.state)){if(!j.output.isEmpty())try{DocumentsContract.deleteDocument(getContentResolver(),Uri.parse(j.output));}catch(Exception ignored){}store.state(j,"cancelled","");}DownloadStore.clearFiles(DownloadStore.directory(this,j));if("remove".equals(action))store.remove(j);}
            }
            if(id!=null)held.remove(id,startId);pauseRequest.compareAndSet(startId,0);
            drain(store,profile);
        }catch(Exception e){state=I18n.text(this,"Could not update downloads. Open Downloads to retry.");}
        finally{pending.decrementAndGet();if(!destroyed)main.post(()->{if(!destroyed&&pending.get()==0){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}});}}});}catch(RejectedExecutionException e){if(id!=null)held.remove(id,startId);pauseRequest.compareAndSet(startId,0);if(pending.decrementAndGet()==0){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}state=I18n.text(this,"Download queue is busy. Please retry.");}
        return START_NOT_STICKY;
    }
    private static String value(Intent i,String key){String v=i.getStringExtra(key);return v==null?"":v;}
    private Notification notification(String title,String message,boolean ongoing){
        PendingIntent content=PendingIntent.getActivity(this,20,new Intent(this,DownloadsActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title).setContentText(message).setOngoing(ongoing).setOnlyAlertOnce(true).setContentIntent(content);
        if(ongoing){PendingIntent action=PendingIntent.getService(this,21,new Intent(this,DownloadService.class).setAction("pause"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);n.addAction(new Notification.Action.Builder(null,I18n.text(this,"Pause"),action).build());n.setProgress(0,0,true);}else n.setAutoCancel(true);
        return n.build();
    }
    private static long downloadScore(YoutubeRepository.Choice c,int maximum){int resolution=YoutubeRepository.resolution(c.label);return resolution<=maximum?100000L+resolution:-resolution;}
    private void check()throws InterruptedIOException{if(destroyed||cancelled||pauseRequest.get()!=0||activeJob!=null&&(held.containsKey(activeJob.id)||!activeJob.profile.equals(((SimpleToobApp)getApplication()).sessions.profile()))||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Paused");}
    private synchronized void progress(String title,String text){if(destroyed||System.currentTimeMillis()-notified<1000)return;notified=System.currentTimeMillis();state=text;notifications.notify(20,notification(title,text,true));}
    private synchronized void stageProgress(String title,String text){notified=0;progress(title,I18n.text(this,text));}
    private void drain(DownloadStore store,String profile){
        for(DownloadStore.Job j:store.list(profile)){if(destroyed||!profile.equals(((SimpleToobApp)getApplication()).sessions.profile()))return;if(pauseRequest.get()!=0){for(DownloadStore.Job rest:store.list(profile))if("queued".equals(rest.state))store.state(rest,"paused","");return;}if(!"queued".equals(j.state)||held.containsKey(j.id))continue;cancelled=false;activeJob=j;store.state(j,"running","");download(store,j);activeJob=null;if(pauseRequest.get()!=0){for(DownloadStore.Job rest:store.list(profile))if("queued".equals(rest.state))store.state(rest,"paused","");return;}}
    }
    private void download(DownloadStore store,DownloadStore.Job job){
        PowerManager.WakeLock wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SimpleToob:download");wake.acquire(6L*60*60*1000);
        File temp=DownloadStore.directory(this,job);SimpleToobApp app=(SimpleToobApp)getApplication();
        try{
            check();if(app.prefs.flag("download_unmetered",false)&&getSystemService(android.net.ConnectivityManager.class).isActiveNetworkMetered())throw new IOException("Unmetered connection required");
            if(!temp.isDirectory()&&!temp.mkdirs())throw new IOException("Cannot create download directory");
            stageProgress(job.title,"Refreshing download links…");app.youtube.invalidateMedia(job.video);YoutubeRepository.Media media=app.youtube.media(job.video);check();
            if(!job.track.isEmpty())media=app.youtube.selectAudioTrack(job.video,job.track);
            int preferred=720;try{preferred=Integer.parseInt(app.prefs.text("quality","720"));}catch(NumberFormatException ignored){}final int maximum=preferred;final String wanted=job.choice;YoutubeRepository.Choice choice=null;
            for(YoutubeRepository.Choice c:media.choices)if(c.downloadable()&&(wanted.isEmpty()||wanted.equals(c.label))){if(choice==null||downloadScore(c,maximum)>downloadScore(choice,maximum))choice=c;}
            if(choice==null)throw new IOException("Selected MP4 format is unavailable");job.choice=choice.label;job.track=media.audioTrackId;
            if(job.output.isEmpty()){
                Uri tree=Uri.parse(job.tree),parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
                String filename=(job.title+" ["+job.video+"]").replaceAll("[\\/:*?\"<>|]","_");if(filename.length()>180)filename=filename.substring(0,180);
                Uri created=DocumentsContract.createDocument(getContentResolver(),parent,"video/mp4",filename+".mp4");if(created==null)throw new IOException("Cannot create destination");job.output=created.toString();
            }
            store.state(job,"running","");Uri output=Uri.parse(job.output);if(!"content".equals(output.getScheme()))throw new IOException("Document permission required");
            File v=new File(temp,"video.mp4"),a=new File(temp,"audio.m4a");List<RangeTransfer.FileSource> tracks=new ArrayList<>();String identity=job.video+":"+job.choice+":"+job.track+":"+choice.codec;
            tracks.add(new RangeTransfer.FileSource(choice.videoUrl,v,identity+":video"));if(choice.separate())tracks.add(new RangeTransfer.FileSource(choice.audioUrl,a,identity+":audio"));
            final long started=SystemClock.elapsedRealtime();
            RangeTransfer t=new RangeTransfer(WebDownloader::mediaUrl,WebDownloader::mediaUserAgent,()->cancelled||destroyed||pauseRequest.get()!=0||held.containsKey(job.id)||!job.profile.equals(app.sessions.profile()),(complete,total,network)->progress(job.title,String.format(Locale.ROOT,"%.1f MB%s · %.2f MB/s",complete/1048576.0,total>0?String.format(Locale.ROOT," / %.1f MB",total/1048576.0):"",network/1048576.0/Math.max(.1,(SystemClock.elapsedRealtime()-started)/1000.0))),()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
            transfer=t;try(t){t.download(tracks,app.prefs.number("download_connections",2));}finally{transfer=null;}
            check();File result=v;if(choice.separate()){result=new File(temp,"merged.mp4");stageProgress(job.title,"Joining audio and video…");mux(v,a,result);}
            check();stageProgress(job.title,"Saving file…");
            try(InputStream in=new FileInputStream(result);OutputStream out=getContentResolver().openOutputStream(output,"wt")){if(out==null)throw new IOException("Cannot write destination");byte[] b=new byte[128*1024];int n;while((n=in.read(b))!=-1){check();out.write(b,0,n);}out.flush();}
            store.state(job,"complete","");DownloadStore.clearFiles(temp);state=I18n.text(this,"Download complete");
        }catch(Exception e){String status=cancelled||destroyed||pauseRequest.get()!=0||held.containsKey(job.id)||!job.profile.equals(app.sessions.profile())||Thread.currentThread().isInterrupted()?"paused":"failed";store.state(job,status, status.equals("failed")?"Download failed. Resume to retry; unavailable formats may need a new download.":"");state=I18n.text(this,status.equals("paused")?"Download paused":"Download failed");}
        finally{RangeTransfer t=transfer;if(t!=null)t.close();transfer=null;if(wake.isHeld())wake.release();if(!destroyed)notifications.notify(21,notification(job.title,state,false));}
    }
    private void mux(File video,File audio,File result)throws IOException{
        MediaExtractor v=new MediaExtractor(),a=new MediaExtractor();MediaMuxer mux=null;
        try{v.setDataSource(video.toString());a.setDataSource(audio.toString());int vi=track(v,"video/"),ai=track(a,"audio/");if(vi<0||ai<0)throw new IOException("Missing audio or video track");mux=new MediaMuxer(result.toString(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);int vo=mux.addTrack(v.getTrackFormat(vi)),ao=mux.addTrack(a.getTrackFormat(ai));v.selectTrack(vi);a.selectTrack(ai);mux.start();ByteBuffer samples=ByteBuffer.allocateDirect(16*1024*1024);copyTrack(v,mux,vo,samples);copyTrack(a,mux,ao,samples);mux.stop();}
        finally{v.release();a.release();if(mux!=null)mux.release();}
    }
    private int track(MediaExtractor e,String prefix){for(int i=0;i<e.getTrackCount();i++){String mime=e.getTrackFormat(i).getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith(prefix))return i;}return -1;}
    private void copyTrack(MediaExtractor e,MediaMuxer mux,int track,ByteBuffer buffer)throws IOException{MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();while(true){check();buffer.clear();int size=e.readSampleData(buffer,0);if(size<0)return;if(size>buffer.capacity())throw new IOException("Media sample exceeds buffer limit");if((e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Encrypted tracks are unsupported");info.set(0,size,e.getSampleTime(),(e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_SYNC)!=0?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);mux.writeSampleData(track,buffer,info);e.advance();}}
    @Override public void onTimeout(int startId,int type){cancelled=true;RangeTransfer t=transfer;if(t!=null)t.close();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){destroyed=true;cancelled=true;RangeTransfer t=transfer;if(t!=null)t.close();worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
