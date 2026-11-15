package com.simpletoob.app;

import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Lifecycle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** Uses synthetic Home data, never a live account. Run on phones, wide foldables and RTL configurations. */
@RunWith(AndroidJUnit4.class)
public final class HomeTest {
    @Test public void nativeHomeRecyclesRecreatesAndClearsOnSignOut()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();SimpleToobApp app=(SimpleToobApp)context.getApplicationContext();HomeRecommendations original=app.home;SharedPreferences preferences=context.getSharedPreferences("preferences",0);java.util.Map<String,?> saved=new java.util.HashMap<>(preferences.getAll());AtomicInteger requests=new AtomicInteger();String profile=app.sessions.profile();AtomicReference<HomeRecommendations.Session> session=new AtomicReference<>(new HomeRecommendations.Session(profile,"home-test",0,"en","US",true));
        app.home=new HomeRecommendations(session::get,(p,r)->token->{requests.incrementAndGet();JSONArray cards=new JSONArray();for(String id:new String[]{"AbCde_12-34","ZbCde_12-34"})cards.put(new JSONObject().put("videoRenderer",new JSONObject().put("videoId",id).put("title",new JSONObject().put("simpleText","Home stays simple")).put("lengthText",new JSONObject().put("simpleText","5:00"))));return new JSONObject().put("contents",new JSONObject().put("richGridRenderer",new JSONObject().put("contents",cards)));});
        preferences.edit().putString("ui_language","en").putBoolean("home_grid",true).putBoolean("compact",false).apply();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context,MainActivity.class).putExtra("open_screen","Home").putExtra("home_mode",HomePolicy.FOR_YOU))){
            awaitRows(scenario);assertEquals(1,requests.get());scenario.onActivity(activity->{AbsListView feed=activity.findViewById(R.id.home_feed);assertNotNull(feed);int columns=HomePolicy.columns(activity.getResources().getConfiguration().screenWidthDp,false,true);if(columns>1){assertTrue(feed instanceof GridView);assertEquals(columns,((GridView)feed).getNumColumns());}else assertTrue(feed instanceof ListView);});
            scenario.recreate();awaitRows(scenario);assertEquals(1,requests.get());scenario.moveToState(Lifecycle.State.STARTED);session.set(new HomeRecommendations.Session(profile,"home-test-logout",0,"en","US",false));scenario.moveToState(Lifecycle.State.RESUMED);InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(activity->{assertNull(activity.findViewById(R.id.home_feed));assertFalse(activity.isFinishing());});
        }finally{app.home.clear();app.home=original;SharedPreferences.Editor edit=preferences.edit();for(String key:new String[]{"ui_language","home_grid","compact",HomePolicy.preference(profile,"mode"),HomePolicy.preference(profile,"chart")}){Object old=saved.get(key);if(old==null)edit.remove(key);else if(old instanceof Boolean)edit.putBoolean(key,(Boolean)old);else if(old instanceof String)edit.putString(key,(String)old);}edit.commit();}
    }
    private static void awaitRows(ActivityScenario<MainActivity> scenario)throws Exception{long end=android.os.SystemClock.elapsedRealtime()+5000;AtomicBoolean ready=new AtomicBoolean();while(android.os.SystemClock.elapsedRealtime()<end){scenario.onActivity(activity->{AbsListView feed=activity.findViewById(R.id.home_feed);if(feed!=null&&feed.getAdapter()!=null&&feed.getAdapter().getCount()>=2)ready.set(true);});if(ready.get())return;Thread.sleep(50);}fail("Home rows did not appear");}
}
