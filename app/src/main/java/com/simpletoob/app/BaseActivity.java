package com.simpletoob.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

abstract class BaseActivity extends Activity {
    SimpleToobApp app; Ui ui;private long seenImportVersion;private final Runnable importListener=()->{long current=app.imports.version();if(current!=seenImportVersion){seenImportVersion=current;onImportsChanged();}else onImportStatusChanged();};
    private volatile boolean activityDestroyed;private String appliedLanguage;
    private final java.util.List<Future<?>> tasks=new java.util.ArrayList<>();
    private final android.os.Handler results=new android.os.Handler(android.os.Looper.getMainLooper());
    @Override protected void onDestroy(){activityDestroyed=true;for(Future<?> task:tasks)task.cancel(true);tasks.clear();results.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(AppLanguage.wrap(base));}
    String tr(int id,Object...args){return I18n.get(this,id,args);}
    String tx(String source){return I18n.text(this,source);}
    protected void onImportStatusChanged(){}
    protected void onImportsChanged(){}
    @Override protected void onResume(){super.onResume();if(appliedLanguage!=null&&!appliedLanguage.equals(AppLanguage.effective(this))){recreate();return;}if(app.imports!=null){seenImportVersion=app.imports.version();app.imports.listen(importListener);}}
    @Override protected void onPause(){if(app.imports!=null)app.imports.unlisten(importListener);super.onPause();}
    @Override protected void onCreate(Bundle state) {
        appliedLanguage=AppLanguage.effective(this);app=(SimpleToobApp)getApplication(); ui=new Ui(this,app.prefs);
        setTheme(ui.dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(state);
    }
    void toast(String message) { Toast.makeText(this,tx(message),Toast.LENGTH_SHORT).show(); }
    void error(String message) { new AlertDialog.Builder(this).setTitle("SimpleToob").setMessage(tx(message)).setPositiveButton(tr(R.string.ui_ok_9ce3bd),null).show(); }
    void external(String url) { WebSessionActivity.open(this,url); }
    void browser(String url) { WebSessionActivity.open(this,url); }
    <T> Future<?> work(ExecutorService executor,Callable<T> action,Consumer<T> success,Consumer<String> failure) {
        try { tasks.removeIf(Future::isDone);Future<?> task=executor.submit(()-> {
            try {
                T result=action.call();
                if(!activityDestroyed&&!Thread.currentThread().isInterrupted()) results.post(()-> { if(!isFinishing() && !isDestroyed()) success.accept(result); });
            } catch(Exception e) {
                if(!activityDestroyed&&!Thread.currentThread().isInterrupted()) {
                    String message=(e instanceof YoutubeRepository.ApiException||e instanceof HomeRecommendations.FeedException)?e.getMessage():"Couldn't complete that action. Please retry.";
                    results.post(()-> { if(!isFinishing() && !isDestroyed()) failure.accept(tx(message)); });
                }
            }
        }); tasks.add(task);return task; } catch(RejectedExecutionException e) { failure.accept(tx("Busy finishing earlier requests. Please retry in a moment.")); return null; }
    }
}
