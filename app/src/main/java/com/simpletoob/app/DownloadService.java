package com.simpletoob.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.*;

/** One cancellable foreground download. Split MP4 tracks are muxed before the output is committed. */
public final class DownloadService extends Service {
    public static volatile String state="";
    private static final String CHANNEL="downloads";
    private final ExecutorService worker=WorkPools.create("simpletoob-download",1,2,()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
    private volatile boolean cancelled,busy;
    private volatile RangeTransfer transfer;
    private long notified;
    private NotificationManager notifications;
    @Override public void onCreate(){super.onCreate();notifications=getSystemService(NotificationManager.class);notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"Video downloads",NotificationManager.IMPORTANCE_LOW));}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if("cancel".equals(intent.getAction())){cancelled=true;RangeTransfer t=transfer;if(t!=null)t.close();return START_NOT_STICKY;}
        if(busy){new Handler(Looper.getMainLooper()).post(()->android.widget.Toast.makeText(this,"A download is already running. Cancel it before starting another.",android.widget.Toast.LENGTH_LONG).show());return START_NOT_STICKY;}
        busy=true;cancelled=false;String title=intent.getStringExtra("title"),video=intent.getStringExtra("video"),audio=intent.getStringExtra("audio"),out=intent.getStringExtra("output");
        state="Starting download…";startForeground(20,notification(title,"Starting download…",true,false,null));
        worker.execute(()->download(title,video,audio,out));return START_NOT_STICKY;
    }
    private Notification notification(String title,String message,boolean ongoing,boolean done,Uri output){
        Intent open=new Intent(this,MainActivity.class);PendingIntent content=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title==null?"SimpleToob download":title).setContentText(message).setOngoing(ongoing).setOnlyAlertOnce(true).setContentIntent(content);
        if(ongoing){Intent cancel=new Intent(this,DownloadService.class).setAction("cancel");PendingIntent action=PendingIntent.getService(this,2,cancel,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);n.addAction(new Notification.Action.Builder(null,"Cancel",action).build());n.setProgress(0,0,true);}
        if(done&&output!=null){Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(output,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);PendingIntent action=PendingIntent.getActivity(this,3,view,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);n.setContentIntent(action).setAutoCancel(true).setSmallIcon(android.R.drawable.stat_sys_download_done);}
        return n.build();
    }
    private void check()throws InterruptedIOException{if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");}
    private synchronized void progress(String title,String text){if(System.currentTimeMillis()-notified<1000)return;notified=System.currentTimeMillis();state=text;notifications.notify(20,notification(title,text,true,false,null));}
    private synchronized void stageProgress(String title,String text){notified=0;progress(title,text);}
    private void download(String title,String video,String audio,String destination){
        PowerManager.WakeLock wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SimpleToob:download");wake.acquire(6L*60*60*1000);
        File temp=new File(getCacheDir(),"download-"+UUID.randomUUID());Uri output=null;boolean success=false;String message="Download failed";
        try{
            if(((SimpleToobApp)getApplication()).prefs.flag("download_unmetered",false)&&getSystemService(android.net.ConnectivityManager.class).isActiveNetworkMetered())throw new IOException("Unmetered-only downloads are enabled. Connect to an unmetered network or change Settings.");
            if(!WebDownloader.mediaUrl(video)||(audio!=null&&!audio.isEmpty()&&!WebDownloader.mediaUrl(audio)))throw new IOException("Unsupported media source");
            output=Uri.parse(destination);if(!"content".equals(output.getScheme()))throw new IOException("Choose a document destination");if(!temp.mkdirs())throw new IOException("Could not create temporary files");
            File v=new File(temp,"video.mp4"),a=new File(temp,"audio.m4a");boolean split=audio!=null&&!audio.isEmpty();
            java.util.List<RangeTransfer.FileSource> tracks=new java.util.ArrayList<>();tracks.add(new RangeTransfer.FileSource(video,v));if(split)tracks.add(new RangeTransfer.FileSource(audio,a));
            long started=SystemClock.elapsedRealtime();transfer=new RangeTransfer(WebDownloader::mediaUrl,WebDownloader::mediaUserAgent,()->cancelled,(complete,total,network)->{double seconds=Math.max(0.1,(SystemClock.elapsedRealtime()-started)/1000.0),rate=network/seconds;String text=String.format(java.util.Locale.ROOT,"%.1f MB%s · %.2f MB/s",complete/1048576.0,total>0?String.format(java.util.Locale.ROOT," / %.1f MB",total/1048576.0):"",rate/1048576.0);if(total>complete&&rate>0)text+=" · "+(long)((total-complete)/rate)+"s left";progress(title,text);},()->android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND));
            transfer.download(tracks,((SimpleToobApp)getApplication()).prefs.number("download_connections",2));transfer=null;File finalFile=v;
            if(split){finalFile=new File(temp,"merged.mp4");stageProgress(title,"Joining audio and video…");mux(v,a,finalFile);}
            check();stageProgress(title,"Saving file…");
            try(InputStream in=new FileInputStream(finalFile);OutputStream out=getContentResolver().openOutputStream(output,"w")){if(out==null)throw new IOException("Cannot write destination");byte[] b=new byte[128*1024];int n;while((n=in.read(b))!=-1){check();out.write(b,0,n);}out.flush();}
            success=true;message="Download complete";
        }catch(Exception e){message=cancelled?"Download cancelled":"Download failed: "+(e instanceof IOException?e.getMessage():"couldn't combine or save this format");}
        finally{
            if(transfer!=null)transfer.close();transfer=null;if(temp.exists()){File[] files=temp.listFiles();if(files!=null)for(File f:files)f.delete();temp.delete();}
            if(!success&&output!=null)try{DocumentsContract.deleteDocument(getContentResolver(),output);}catch(Exception ignored){}
            if(wake.isHeld())wake.release();state=message;final String resultMessage=message;final boolean completed=success;final Uri document=output;
            new Handler(Looper.getMainLooper()).post(()->{busy=false;stopForeground(STOP_FOREGROUND_REMOVE);notifications.notify(21,notification(title,resultMessage,false,completed,document));stopSelf();});
        }
    }
    private void mux(File video,File audio,File result)throws IOException{
        MediaExtractor v=new MediaExtractor(),a=new MediaExtractor();MediaMuxer mux=null;
        try{v.setDataSource(video.toString());a.setDataSource(audio.toString());int vi=track(v,"video/"),ai=track(a,"audio/");if(vi<0||ai<0)throw new IOException("Missing audio or video track");mux=new MediaMuxer(result.toString(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);int vo=mux.addTrack(v.getTrackFormat(vi)),ao=mux.addTrack(a.getTrackFormat(ai));v.selectTrack(vi);a.selectTrack(ai);mux.start();copyTrack(v,mux,vo);copyTrack(a,mux,ao);mux.stop();}
        finally{v.release();a.release();if(mux!=null)mux.release();}
    }
    private int track(MediaExtractor e,String prefix){for(int i=0;i<e.getTrackCount();i++){String mime=e.getTrackFormat(i).getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith(prefix))return i;}return -1;}
    private void copyTrack(MediaExtractor e,MediaMuxer mux,int track)throws IOException{ByteBuffer buffer=ByteBuffer.allocateDirect(16*1024*1024);MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();while(true){check();buffer.clear();int size=e.readSampleData(buffer,0);if(size<0)return;if(size>buffer.capacity())throw new IOException("Media sample exceeds buffer limit");if((e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Encrypted tracks are unsupported");info.set(0,size,e.getSampleTime(),(e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_SYNC)!=0?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);mux.writeSampleData(track,buffer,info);e.advance();}}
    @Override public void onTimeout(int startId,int type){cancelled=true;RangeTransfer t=transfer;if(t!=null)t.close();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){cancelled=true;RangeTransfer t=transfer;if(t!=null)t.close();worker.shutdownNow();super.onDestroy();}
}
