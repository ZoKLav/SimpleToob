package com.simpletoob.app;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
/** Runs the actual native player on a bundled H.264 clip; no network/account needed. */
@RunWith(AndroidJUnit4.class)
public final class RendererTest {
    @Test public void baselineVideoRendersNonBlackPixels()throws Exception{
        Diagnostics.clear();Intent intent=new Intent(ApplicationProvider.getApplicationContext(),PlayerActivity.class).putExtra("renderer_test",true).putExtra("test_surface","SurfaceView");
        try(ActivityScenario<PlayerActivity> scenario=ActivityScenario.launch(intent)){
            long deadline=SystemClock.elapsedRealtime()+15000;
            while(!Diagnostics.report().contains("first video frame rendered")&&SystemClock.elapsedRealtime()<deadline)Thread.sleep(50);
            assertTrue("No rendered frame: "+Diagnostics.report(),Diagnostics.report().contains("first video frame rendered"));
            Bitmap image=Bitmap.createBitmap(80,45,Bitmap.Config.ARGB_8888);CountDownLatch copied=new CountDownLatch(1);AtomicInteger result=new AtomicInteger(-1);
            scenario.onActivity(activity->{SurfaceView surface=activity.getWindow().getDecorView().findViewWithTag("native_video_surface");assertNotNull(surface);PixelCopy.request(surface,image,code->{result.set(code);copied.countDown();},new Handler(Looper.getMainLooper()));});
            assertTrue(copied.await(3,TimeUnit.SECONDS));assertEquals(PixelCopy.SUCCESS,result.get());Set<Integer> colors=new HashSet<>();for(int y=0;y<image.getHeight();y+=5)for(int x=0;x<image.getWidth();x+=5)colors.add(image.getPixel(x,y));assertTrue("Rendered image is uniform/black",colors.size()>10);image.recycle();
        }
    }
}
