package com.simpletoob.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

/** Two bounded transfer workers shared across tracks; strict ranges and safe whole-file fallback. */
final class RangeTransfer implements AutoCloseable {
    interface Progress{void update(long complete,long total,long transferred);}
    static final class FileSource {final String url;final File file;long size=-1,next;String etag="";boolean ranges,claimed;FileSource(String url,File file){this.url=url;this.file=file;}}
    private static final int CHUNK=1024*1024;
    private final Predicate<String> allowed;private final Function<String,String> userAgent;private final BooleanSupplier cancelled;private final Progress progress;private final Runnable workerSetup;
    private final Set<HttpURLConnection> active=ConcurrentHashMap.newKeySet();
    private final AtomicLong completed=new AtomicLong(),transferred=new AtomicLong();
    private volatile boolean stopped;private volatile IOException failure;private long total;private int turn;
    RangeTransfer(Predicate<String> allowed,Function<String,String> userAgent,BooleanSupplier cancelled,Progress progress){this(allowed,userAgent,cancelled,progress,()->{});}
    RangeTransfer(Predicate<String> allowed,Function<String,String> userAgent,BooleanSupplier cancelled,Progress progress,Runnable workerSetup){this.workerSetup=workerSetup;this.allowed=allowed;this.userAgent=userAgent;this.cancelled=cancelled;this.progress=progress;}
    private void check()throws InterruptedIOException{if(stopped||cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");}
    private HttpURLConnection open(String source,String range,String etag)throws IOException{
        String url=source;for(int redirect=0;redirect<6;redirect++){check();if(!allowed.test(url))throw new IOException("Unsupported media redirect");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();active.add(c);c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent",userAgent.apply(url));c.setRequestProperty("Referer","https://www.youtube.com/");c.setRequestProperty("Origin","https://www.youtube.com");c.setRequestProperty("Accept-Encoding","identity");if(range!=null)c.setRequestProperty("Range",range);if(!etag.isEmpty())c.setRequestProperty("If-Match",etag);
            int code;try{code=c.getResponseCode();check();}catch(IOException e){release(c);throw e;}
            if(code==301||code==302||code==303||code==307||code==308){String location=c.getHeaderField("Location");release(c);if(location==null)throw new IOException("Invalid media redirect");url=new URL(new URL(url),location).toString();continue;}return c;
        }throw new IOException("Too many media redirects");
    }
    private void release(HttpURLConnection c){c.disconnect();active.remove(c);}
    private void probe(FileSource file)throws IOException{
        HttpURLConnection c=open(file.url,"bytes=0-0","");try{int code=c.getResponseCode();if(code==206){long[] range=ByteRanges.parse(c.getHeaderField("Content-Range"));if(range==null||range[0]!=0||range[1]!=0)throw new IOException("Invalid media range probe");file.ranges=true;file.size=range[2];try(InputStream in=c.getInputStream()){if(in.read()<0||in.read()!=-1)throw new IOException("Invalid media range body");}}
            else if(code==200){file.size=c.getContentLengthLong();file.ranges=false;}
            else throw new IOException("Media server returned "+code+"; retry the video to refresh its link");
            String etag=c.getHeaderField("ETag");if(etag!=null&&!etag.startsWith("W/"))file.etag=etag;
        }finally{release(c);}
    }
    void download(List<FileSource> files,int concurrency)throws IOException{
        if(files.isEmpty())throw new IOException("No media tracks");total=0;boolean known=true;for(FileSource file:files){probe(file);if(file.size<=0)known=false;else{if(Long.MAX_VALUE-total<file.size)throw new IOException("Media size overflow");total+=file.size;}}
        // Conservative peak also reserves a destination copy on the same storage volume.
        long copies=files.size()>1?3:2;if(total>0&&(total>(Long.MAX_VALUE-32L*1024*1024)/copies||copies*total+32L*1024*1024>files.get(0).file.getParentFile().getUsableSpace()))throw new IOException("Not enough temporary storage");if(!known)total=-1;
        ExecutorService workers=Executors.newFixedThreadPool(Math.max(1,Math.min(3,concurrency)),work->new Thread(()->{workerSetup.run();work.run();},"simpletoob-download-part"));List<Future<?>> jobs=new ArrayList<>();
        try{for(int n=0;n<Math.max(1,Math.min(3,concurrency));n++)jobs.add(workers.submit(()->{try{while(true){check();Task task=next(files);if(task==null)return;if(task.file.ranges)range(task);else whole(task.file);}}catch(IOException e){synchronized(this){if(failure==null)failure=e;}closeConnections();}}));for(Future<?> job:jobs)try{job.get();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException("Cancelled");}catch(ExecutionException e){throw new IOException("Transfer worker failed",e.getCause());}if(failure!=null)throw failure;check();for(FileSource file:files)if(file.size>=0&&file.file.length()!=file.size)throw new IOException("Media download ended early");}
        finally{stopped=true;closeConnections();workers.shutdownNow();boolean interrupted=Thread.interrupted(),terminated=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);try{while(!terminated&&System.nanoTime()<deadline){try{terminated=workers.awaitTermination(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}catch(InterruptedException e){interrupted=true;}}if(!terminated)throw new IOException("Transfer cancellation timed out");}finally{if(interrupted)Thread.currentThread().interrupt();}}
    }
    private static final class Task{final FileSource file;final long start,end;Task(FileSource file,long start,long end){this.file=file;this.start=start;this.end=end;}}
    private synchronized Task next(List<FileSource> files){for(int i=0;i<files.size();i++){FileSource f=files.get(turn++%files.size());if(f.ranges&&f.next<f.size){long start=f.next,end=Math.min(f.size-1,start+CHUNK-1);f.next=end+1;return new Task(f,start,end);}if(!f.ranges&&!f.claimed){f.claimed=true;return new Task(f,0,-1);}}return null;}
    private void range(Task task)throws IOException{
        IOException last=null;for(int attempt=0;attempt<3;attempt++){check();HttpURLConnection c=null;try{c=open(task.file.url,"bytes="+task.start+"-"+task.end,task.file.etag);int code=c.getResponseCode();long[] r=ByteRanges.parse(c.getHeaderField("Content-Range"));if(code!=206||r==null||r[0]!=task.start||r[1]!=task.end||r[2]!=task.file.size)throw new IOException("Media server changed or ignored a verified byte range");String tag=c.getHeaderField("ETag");if(!task.file.etag.isEmpty()&&tag!=null&&!task.file.etag.equals(tag))throw new IOException("Media changed during transfer");long read=0,want=task.end-task.start+1;try(InputStream in=c.getInputStream();RandomAccessFile out=new RandomAccessFile(task.file.file,"rw")){out.seek(task.start);byte[] buffer=new byte[64*1024];int n;while((n=in.read(buffer,0,(int)Math.min(buffer.length,want-read+1)))!=-1){check();if(read+n>want)throw new IOException("Oversized media range");out.write(buffer,0,n);read+=n;transferred.addAndGet(n);report();}if(read!=want)throw new EOFException("Media range ended early");}completed.addAndGet(want);report();return;}
            catch(IOException e){last=e;if(c!=null){int code;try{code=c.getResponseCode();}catch(IOException ignored){code=0;}if(code==401||code==403||code==404||code==412||code==416)throw e;}if(attempt<2)try{Thread.sleep(200L*(attempt+1));}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new InterruptedIOException("Cancelled");}}
            finally{if(c!=null)release(c);}}
        throw last;
    }
    private void whole(FileSource file)throws IOException{
        HttpURLConnection c=open(file.url,null,file.etag);try{if(c.getResponseCode()!=200)throw new IOException("Media server returned "+c.getResponseCode());long expected=c.getContentLengthLong();if(file.size>0&&expected>0&&file.size!=expected)throw new IOException("Media changed during transfer");long read=0;try(InputStream in=c.getInputStream();OutputStream out=new BufferedOutputStream(new FileOutputStream(file.file))){byte[] buffer=new byte[128*1024];int n;while((n=in.read(buffer))!=-1){check();if(n+32L*1024*1024>file.file.getParentFile().getUsableSpace())throw new IOException("Not enough temporary storage");out.write(buffer,0,n);read+=n;transferred.addAndGet(n);completed.addAndGet(n);report();}}if(expected>=0&&read!=expected||file.size>=0&&read!=file.size)throw new EOFException("Media download ended early");}finally{release(c);}
    }
    private void report(){progress.update(completed.get(),total,transferred.get());}
    private void closeConnections(){stopped=true;for(HttpURLConnection c:active)c.disconnect();}
    @Override public void close(){closeConnections();}
}
