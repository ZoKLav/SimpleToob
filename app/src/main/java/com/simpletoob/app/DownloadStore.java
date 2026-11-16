package com.simpletoob.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** A small durable queue with no stream URLs or Activity references. */
final class DownloadStore extends SQLiteOpenHelper {
    static final int LIMIT=500;
    static final class Job {String id,profile,video,title,choice,track,output,tree,state,message;}
    DownloadStore(Context context){this(context,"downloads.db");}
    DownloadStore(Context context,String database){super(context.getApplicationContext(),database,null,1);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE jobs(id TEXT PRIMARY KEY,profile TEXT NOT NULL,video TEXT NOT NULL,title TEXT NOT NULL,choice TEXT NOT NULL,track TEXT NOT NULL,output TEXT NOT NULL,tree TEXT NOT NULL,state TEXT NOT NULL,message TEXT NOT NULL,created INTEGER NOT NULL)");}
    public void onUpgrade(SQLiteDatabase db,int from,int to){}
    void recover(){getWritableDatabase().execSQL("UPDATE jobs SET state='paused',message='' WHERE state='running'");}
    List<Job> list(String profile){List<Job> jobs=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,profile,video,title,choice,track,output,tree,state,message FROM jobs WHERE profile=? ORDER BY created,rowid",new String[]{profile})){while(c.moveToNext()){Job j=new Job();j.id=c.getString(0);j.profile=c.getString(1);j.video=c.getString(2);j.title=c.getString(3);j.choice=c.getString(4);j.track=c.getString(5);j.output=c.getString(6);j.tree=c.getString(7);j.state=c.getString(8);j.message=c.getString(9);jobs.add(j);}}return jobs;}
    void enqueue(String profile,List<Video> videos,String output,String tree,String choice,String track)throws IOException{
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{List<Job> old=list(profile);if(videos.isEmpty()||videos.size()+old.size()>LIMIT)throw new IOException("Download queue is full. Remove completed downloads first.");Set<String> active=new HashSet<>();for(Job j:old)if(!j.state.equals("complete")&&!j.state.equals("cancelled"))active.add(j.video);long now=System.currentTimeMillis();for(Video v:videos){if(!VideoIds.valid(v.id))continue;if(!active.add(v.id))continue;ContentValues row=new ContentValues();row.put("id",UUID.randomUUID().toString());row.put("profile",profile);row.put("video",v.id);row.put("title",v.title);row.put("choice",choice);row.put("track",track);row.put("output",output);row.put("tree",tree);row.put("state","queued");row.put("message","");row.put("created",now++);db.insertOrThrow("jobs",null,row);}db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    void state(Job job,String state,String message){job.state=state;job.message=message;ContentValues row=new ContentValues();row.put("state",state);row.put("message",message);row.put("output",job.output);row.put("choice",job.choice);row.put("track",job.track);getWritableDatabase().update("jobs",row,"id=?",new String[]{job.id});}
    void remove(Job job){getWritableDatabase().delete("jobs","id=?",new String[]{job.id});}
    static File directory(Context c,Job j){return new File(c.getFilesDir(),"downloads/"+j.id);}
    static void clearFiles(File file){File[] files=file.listFiles();if(files!=null)for(File f:files)f.delete();file.delete();}
}
