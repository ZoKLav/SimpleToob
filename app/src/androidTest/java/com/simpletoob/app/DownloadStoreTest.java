package com.simpletoob.app;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

/** Real Android queue persistence and profile isolation; never touches the user's download DB. */
@RunWith(AndroidJUnit4.class)
public final class DownloadStoreTest {
    @Test public void jobsSurviveReopenAndInterruptedJobsPause()throws Exception{
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String dbName="downloads-test-"+UUID.randomUUID()+".db";
        try{
            String id;
            try(DownloadStore db=new DownloadStore(c,dbName)){
                Video a=Video.link("abcdefghijk"),b=Video.link("bcdefghijkl");
                db.enqueue("A",Arrays.asList(a,a,b),"","content://test/folder","","");
                assertEquals(2,db.list("A").size());assertTrue(db.list("B").isEmpty());
                db.enqueue("A",Collections.singletonList(a),"","content://test/folder","","");assertEquals(2,db.list("A").size());
                DownloadStore.Job job=db.list("A").get(0);id=job.id;job.output="content://test/output";db.state(job,"running","");
                db.enqueue("B",Collections.singletonList(a),"content://test/other","","","");
            }
            try(DownloadStore db=new DownloadStore(c,dbName)){
                db.recover();List<DownloadStore.Job> a=db.list("A");assertEquals(id,a.get(0).id);assertEquals("paused",a.get(0).state);assertEquals("content://test/output",a.get(0).output);assertEquals("queued",a.get(1).state);assertEquals(1,db.list("B").size());
                db.state(a.get(0),"queued","");db.state(a.get(0),"complete","");db.recover();assertEquals("complete",db.list("A").get(0).state);db.remove(a.get(0));assertEquals(1,db.list("A").size());assertEquals(1,db.list("B").size());
            }
        }finally{c.deleteDatabase(dbName);}
    }
}
