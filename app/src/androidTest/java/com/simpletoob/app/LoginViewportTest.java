package com.simpletoob.app;
import android.webkit.*;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.*;
import static org.junit.Assert.*;
/** Offline page geometry check using exactly the sign-in WebSettings configuration. */
@RunWith(AndroidJUnit4.class)
public final class LoginViewportTest {
    @Test public void mobilePageHardwareViewport()throws Exception{checkViewport(false);}
    @Test public void mobilePageSoftwareViewport()throws Exception{checkViewport(true);}
    private void checkViewport(boolean compatibility)throws Exception{
        CompletableFuture<double[]> geometry=new CompletableFuture<>();final WebView[] page={null};
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{FrameLayout root=new FrameLayout(activity);WebView web=new WebView(activity);page[0]=web;LoginWebSettings.apply(web,100,compatibility);root.addView(web,new FrameLayout.LayoutParams(-1,-1));activity.setContentView(root);web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView view,String url){view.evaluateJavascript("JSON.stringify({width:innerWidth,scroll:document.documentElement.scrollWidth})",value->{try{String raw=new org.json.JSONArray("["+value+"]").getString(0);JSONObject j=new JSONObject(raw);geometry.complete(new double[]{view.getWidth()/activity.getResources().getDisplayMetrics().density,j.getDouble("width"),j.getDouble("scroll")});}catch(Exception e){geometry.completeExceptionally(e);}});}});web.post(()->web.loadDataWithBaseURL("https://viewport.invalid/","<!doctype html><meta name='viewport' content='width=device-width, initial-scale=1'><style>html,body{margin:0}main{box-sizing:border-box;width:100%;padding:16px}section{height:200px;background:#1278aa}</style><main><h1>Offline layout check</h1><section></section></main>","text/html","UTF-8",null));});
            double[] dimensions=geometry.get(15,TimeUnit.SECONDS);assertTrue(dimensions[0]>100);assertEquals("CSS viewport must match available device width",dimensions[0],dimensions[1],3);assertEquals("Page must not overflow horizontally",dimensions[1],dimensions[2],2);
            scenario.onActivity(activity->{((FrameLayout)page[0].getParent()).removeView(page[0]);page[0].destroy();});
        }
    }
}
